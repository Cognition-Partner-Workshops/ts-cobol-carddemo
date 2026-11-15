package com.carddemo.interest.batch;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.RunStops;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.BalanceSource;
import com.carddemo.interest.port.CardLookup;
import com.carddemo.interest.port.RateLookup;
import com.carddemo.interest.port.RunLog;
import com.carddemo.interest.port.TransactionSink;

/**
 * INT-R08 for the JDBC adapters of the ports: a database failure stops the run as the file error
 * it replaces did (CBACT04C:341-345, 356-369, 378-390, 400-412, 422-434, 500-514). The run log
 * gets the legacy message of the operation and no status line, because no legacy status is
 * determined (standards S6.1). The application log gets the class of the failure and its SQLSTATE
 * only: the driver's message can quote the values of a row, so it is neither logged nor kept as
 * the cause (standards S6.5).
 */
final class DatabaseFailureStops {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFailureStops.class);

    private final RunStops stops;

    DatabaseFailureStops(RunLog runLog) {
        this.stops = new RunStops(runLog);
    }

    BalanceSource balances(BalanceSource balances) {
        return () -> stopping(StopReason.READ_BALANCES, balances::nextAccount);
    }

    AccountStore accounts(AccountStore accounts) {
        return new AccountStore() {
            @Override
            public Optional<Account> find(long accountId) {
                return stopping(StopReason.READ_ACCOUNT, () -> accounts.find(accountId));
            }

            @Override
            public void postInterest(long accountId, BigDecimal newBalance) {
                try {
                    accounts.postInterest(accountId, newBalance);
                } catch (DataAccessException e) {
                    throw databaseFailure(StopReason.UPDATE_ACCOUNT, e);
                } catch (InterestRunStoppedException e) {
                    if (e.reason() != StopReason.WRITE_OUTPUT) {
                        throw e;
                    }
                    // the adapter refuses an account break whose account row is gone
                    throw stops.operationFailed(StopReason.UPDATE_ACCOUNT,
                            "the account of an account break is not in the account table");
                }
            }
        };
    }

    CardLookup cards(CardLookup cards) {
        return accountId -> stopping(StopReason.READ_CARD, () -> cards.cardNumbersOf(accountId));
    }

    RateLookup rates(RateLookup rates) {
        return (groupId, typeCode, categoryCode) -> stopping(StopReason.READ_RATE,
                () -> rates.findRate(groupId, typeCode, categoryCode));
    }

    TransactionSink transactions(TransactionSink transactions) {
        return transaction -> stopping(StopReason.WRITE_TRANSACTION, () -> {
            transactions.write(transaction);
            return List.of();
        });
    }

    private <T> T stopping(StopReason reason, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException e) {
            throw databaseFailure(reason, e);
        }
    }

    private InterestRunStoppedException databaseFailure(StopReason reason, DataAccessException failure) {
        log.error("database failure: reason={} failure={} sqlState={}", reason, failure.getClass().getSimpleName(),
                sqlStateOf(failure));
        return stops.operationFailed(reason, "the database refused the operation; SQLSTATE " + sqlStateOf(failure));
    }

    private static String sqlStateOf(DataAccessException failure) {
        return failure.getMostSpecificCause() instanceof SQLException sql && sql.getSQLState() != null
                ? sql.getSQLState() : "unknown";
    }
}
