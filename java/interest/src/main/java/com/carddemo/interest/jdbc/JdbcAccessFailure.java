package com.carddemo.interest.jdbc;

import java.sql.SQLException;

/**
 * A table of the stream cannot be read. The message names the table only, never a key or a value
 * (standards S6.5). Its mapping to the legacy messages of INT-R08 is ticket 08.
 */
public class JdbcAccessFailure extends RuntimeException {

    public JdbcAccessFailure(String message, SQLException cause) {
        super(message, cause);
    }
}
