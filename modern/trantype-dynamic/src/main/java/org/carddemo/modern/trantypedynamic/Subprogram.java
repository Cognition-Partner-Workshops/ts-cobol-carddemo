package org.carddemo.modern.trantypedynamic;

import java.sql.SQLException;

/** A program CBTRNDYN can reach through {@code CALL WS-PGM-NAME}; returns its RETURN-CODE. */
interface Subprogram {
    int call() throws SQLException;
}
