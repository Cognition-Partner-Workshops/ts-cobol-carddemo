package com.aws.carddemo.etl.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * VSAM Export Record Reader - reads 500-byte export records from CBEXPORT.cbl
 * Based on CVEXPORT.cpy structure (lines 31-90)
 * 
 * Record structure:
 * - Bytes 1-10: Record type identifier
 * - Bytes 11-500: Record data (varies by type)
 */
@Slf4j
public class VsamExportRecordReader {

    private static final int RECORD_LENGTH = 500;
    private static final Charset EBCDIC_CHARSET = Charset.forName("IBM037");

    public static FlatFileItemReader<ExportRecord> createReader(Resource resource) {
        FlatFileItemReader<ExportRecord> reader = new FlatFileItemReader<>();
        reader.setResource(resource);
        reader.setEncoding("IBM037");

        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setNames("recordType", "recordData");
        tokenizer.setColumns(new Range(1, 10), new Range(11, 500));

        DefaultLineMapper<ExportRecord> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> {
            ExportRecord record = new ExportRecord();
            record.setRecordType(fieldSet.readString("recordType").trim());
            record.setRecordData(fieldSet.readString("recordData"));
            return record;
        });

        reader.setLineMapper(lineMapper);
        return reader;
    }

    public static CustomerRecord parseCustomerRecord(String data) {
        CustomerRecord record = new CustomerRecord();
        record.setCustomerId(parseLong(data, 0, 10));
        record.setFirstName(parseString(data, 10, 35));
        record.setMiddleName(parseString(data, 35, 60));
        record.setLastName(parseString(data, 60, 85));
        record.setAddressLine1(parseString(data, 85, 135));
        record.setAddressLine2(parseString(data, 135, 185));
        record.setAddressLine3(parseString(data, 185, 235));
        record.setStateCode(parseString(data, 235, 237));
        record.setCountryCode(parseString(data, 237, 240));
        record.setZipCode(parseString(data, 240, 250));
        record.setPhoneNumber1(parseString(data, 250, 265));
        record.setPhoneNumber2(parseString(data, 265, 280));
        record.setSsn(parseLong(data, 280, 289));
        record.setGovtIssuedId(parseString(data, 289, 309));
        record.setDateOfBirth(parseString(data, 309, 319));
        record.setEftAccountId(parseString(data, 319, 329));
        record.setPrimaryCardHolder(parseString(data, 329, 330));
        record.setFicoCreditScore(parseInt(data, 330, 333));
        return record;
    }

    public static AccountRecord parseAccountRecord(String data) {
        AccountRecord record = new AccountRecord();
        record.setAccountId(parseLong(data, 0, 11));
        record.setActiveStatus(parseString(data, 11, 12));
        record.setCurrentBalance(parseDecimal(data, 12, 24, 2));
        record.setCreditLimit(parseDecimal(data, 24, 36, 2));
        record.setCashCreditLimit(parseDecimal(data, 36, 48, 2));
        record.setOpenDate(parseString(data, 48, 58));
        record.setExpirationDate(parseString(data, 58, 68));
        record.setReissueDate(parseString(data, 68, 78));
        record.setCurrentCycleCredit(parseDecimal(data, 78, 90, 2));
        record.setCurrentCycleDebit(parseDecimal(data, 90, 102, 2));
        record.setZipCode(parseString(data, 102, 112));
        record.setGroupId(parseString(data, 112, 122));
        return record;
    }

    public static CardRecord parseCardRecord(String data) {
        CardRecord record = new CardRecord();
        record.setCardNumber(parseString(data, 0, 16));
        record.setAccountId(parseLong(data, 16, 27));
        record.setCvvCode(parseInt(data, 27, 30));
        record.setEmbossedName(parseString(data, 30, 80));
        record.setExpirationDate(parseString(data, 80, 90));
        record.setActiveStatus(parseString(data, 90, 91));
        return record;
    }

    public static TransactionRecord parseTransactionRecord(String data) {
        TransactionRecord record = new TransactionRecord();
        record.setTransactionId(parseString(data, 0, 16));
        record.setTransactionTypeCode(parseString(data, 16, 18));
        record.setTransactionCategoryCode(parseInt(data, 18, 22));
        record.setTransactionSource(parseString(data, 22, 32));
        record.setDescription(parseString(data, 32, 132));
        record.setAmount(parseDecimal(data, 132, 144, 2));
        record.setMerchantId(parseLong(data, 144, 153));
        record.setMerchantName(parseString(data, 153, 203));
        record.setMerchantCity(parseString(data, 203, 228));
        record.setMerchantZip(parseString(data, 228, 238));
        record.setCardNumber(parseString(data, 238, 254));
        record.setOriginTimestamp(parseString(data, 254, 280));
        record.setProcessTimestamp(parseString(data, 280, 306));
        return record;
    }

    private static String parseString(String data, int start, int end) {
        if (data.length() < end) {
            end = data.length();
        }
        if (start >= data.length()) {
            return "";
        }
        return data.substring(start, end).trim();
    }

    private static int parseInt(String data, int start, int end) {
        String value = parseString(data, start, end);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String data, int start, int end) {
        String value = parseString(data, start, end);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal parseDecimal(String data, int start, int end, int scale) {
        String value = parseString(data, start, end);
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal decimal = new BigDecimal(value);
            return decimal.movePointLeft(scale);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @lombok.Getter
    @lombok.Setter
    public static class ExportRecord {
        private String recordType;
        private String recordData;
    }

    @lombok.Getter
    @lombok.Setter
    public static class CustomerRecord {
        private Long customerId;
        private String firstName;
        private String middleName;
        private String lastName;
        private String addressLine1;
        private String addressLine2;
        private String addressLine3;
        private String stateCode;
        private String countryCode;
        private String zipCode;
        private String phoneNumber1;
        private String phoneNumber2;
        private Long ssn;
        private String govtIssuedId;
        private String dateOfBirth;
        private String eftAccountId;
        private String primaryCardHolder;
        private Integer ficoCreditScore;
    }

    @lombok.Getter
    @lombok.Setter
    public static class AccountRecord {
        private Long accountId;
        private String activeStatus;
        private BigDecimal currentBalance;
        private BigDecimal creditLimit;
        private BigDecimal cashCreditLimit;
        private String openDate;
        private String expirationDate;
        private String reissueDate;
        private BigDecimal currentCycleCredit;
        private BigDecimal currentCycleDebit;
        private String zipCode;
        private String groupId;
    }

    @lombok.Getter
    @lombok.Setter
    public static class CardRecord {
        private String cardNumber;
        private Long accountId;
        private Integer cvvCode;
        private String embossedName;
        private String expirationDate;
        private String activeStatus;
    }

    @lombok.Getter
    @lombok.Setter
    public static class TransactionRecord {
        private String transactionId;
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
        private String transactionSource;
        private String description;
        private BigDecimal amount;
        private Long merchantId;
        private String merchantName;
        private String merchantCity;
        private String merchantZip;
        private String cardNumber;
        private String originTimestamp;
        private String processTimestamp;
    }
}
