package com.carddemo.export.serialization;

import com.carddemo.export.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializer for ExportRecord instances to/from fixed-length byte format.
 * Preserves the 500-byte fixed-length constraint from the COBOL CVEXPORT structure.
 * 
 * This serializer handles the conversion between Java objects and the fixed-length
 * record format used for branch migration file transfers.
 * 
 * Record Layout (500 bytes total):
 * - Header (40 bytes):
 *   - Record Type: 1 byte
 *   - Timestamp: 26 bytes (ISO-8601)
 *   - Sequence Number: 4 bytes (binary COMP)
 *   - Branch ID: 4 bytes
 *   - Region Code: 5 bytes
 * - Data Payload: 460 bytes (varies by record type)
 */
public final class ExportRecordSerializer {
    
    public static final int TOTAL_RECORD_SIZE = 500;
    public static final int HEADER_SIZE = 40;
    public static final int DATA_SIZE = 460;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private final Charset charset;
    
    public ExportRecordSerializer() {
        this(StandardCharsets.UTF_8);
    }
    
    public ExportRecordSerializer(Charset charset) {
        this.charset = charset;
    }
    
    public byte[] serialize(ExportRecord record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TOTAL_RECORD_SIZE);
        DataOutputStream dos = new DataOutputStream(baos);
        
        writeHeader(dos, record);
        writeData(dos, record.getData());
        
        byte[] result = baos.toByteArray();
        if (result.length != TOTAL_RECORD_SIZE) {
            throw new IOException("Serialized record size mismatch: expected " + 
                    TOTAL_RECORD_SIZE + ", got " + result.length);
        }
        
        return result;
    }
    
    public ExportRecord deserialize(byte[] bytes) throws IOException {
        if (bytes.length != TOTAL_RECORD_SIZE) {
            throw new IOException("Invalid record size: expected " + 
                    TOTAL_RECORD_SIZE + ", got " + bytes.length);
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        
        char recordTypeCode = (char) dis.readByte();
        RecordType recordType = RecordType.fromCode(recordTypeCode);
        
        String timestampStr = readFixedString(dis, 26);
        Instant timestamp = parseTimestamp(timestampStr);
        
        long sequenceNumber = Integer.toUnsignedLong(dis.readInt());
        
        String branchId = readFixedString(dis, 4);
        String regionCode = readFixedString(dis, 5);
        
        ExportRecordData data = readData(dis, recordType);
        
        return ExportRecord.builder()
                .recordType(recordType)
                .timestamp(timestamp)
                .sequenceNumber(sequenceNumber)
                .branchId(branchId)
                .regionCode(regionCode)
                .data(data)
                .build();
    }
    
    public List<ExportRecord> deserializeAll(InputStream inputStream) throws IOException {
        List<ExportRecord> records = new ArrayList<>();
        byte[] buffer = new byte[TOTAL_RECORD_SIZE];
        
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            if (bytesRead != TOTAL_RECORD_SIZE) {
                throw new IOException("Incomplete record: expected " + 
                        TOTAL_RECORD_SIZE + " bytes, got " + bytesRead);
            }
            records.add(deserialize(buffer));
        }
        
        return records;
    }
    
    public void serializeAll(List<ExportRecord> records, OutputStream outputStream) throws IOException {
        for (ExportRecord record : records) {
            outputStream.write(serialize(record));
        }
    }
    
    private void writeHeader(DataOutputStream dos, ExportRecord record) throws IOException {
        dos.writeByte(record.getRecordType().getCode());
        
        String timestamp = formatTimestamp(record.getTimestamp());
        writeFixedString(dos, timestamp, 26);
        
        dos.writeInt(record.getSequenceNumber().intValue());
        
        writeFixedString(dos, record.getBranchId(), 4);
        writeFixedString(dos, record.getRegionCode(), 5);
    }
    
    private void writeData(DataOutputStream dos, ExportRecordData data) throws IOException {
        ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream(DATA_SIZE);
        DataOutputStream dataDos = new DataOutputStream(dataBuffer);
        
        switch (data) {
            case CustomerExportData customer -> writeCustomerData(dataDos, customer);
            case AccountExportData account -> writeAccountData(dataDos, account);
            case CardXrefExportData cardXref -> writeCardXrefData(dataDos, cardXref);
            case TransactionExportData transaction -> writeTransactionData(dataDos, transaction);
            case CardExportData card -> writeCardData(dataDos, card);
        }
        
        byte[] dataBytes = dataBuffer.toByteArray();
        dos.write(dataBytes);
        
        int padding = DATA_SIZE - dataBytes.length;
        if (padding > 0) {
            dos.write(new byte[padding]);
        }
    }
    
    private void writeCustomerData(DataOutputStream dos, CustomerExportData customer) throws IOException {
        dos.writeInt(customer.getCustomerId().intValue());
        writeFixedString(dos, customer.getFirstName(), 25);
        writeFixedString(dos, customer.getMiddleName(), 25);
        writeFixedString(dos, customer.getLastName(), 25);
        
        for (int i = 0; i < 3; i++) {
            writeFixedString(dos, customer.getAddressLine(i), 50);
        }
        
        writeFixedString(dos, customer.getStateCode(), 2);
        writeFixedString(dos, customer.getCountryCode(), 3);
        writeFixedString(dos, customer.getZipCode(), 10);
        
        for (int i = 0; i < 2; i++) {
            writeFixedString(dos, customer.getPhoneNumber(i), 15);
        }
        
        writeFixedString(dos, customer.getSsn(), 9);
        writeFixedString(dos, customer.getGovernmentIssuedId(), 20);
        writeFixedString(dos, customer.getFormattedDateOfBirth(), 10);
        writeFixedString(dos, customer.getEftAccountId(), 10);
        writeFixedString(dos, customer.getPrimaryCardHolderIndicator(), 1);
        
        writePackedDecimal(dos, customer.getFicoCreditScore(), 2);
    }
    
    private void writeAccountData(DataOutputStream dos, AccountExportData account) throws IOException {
        writeFixedString(dos, account.getAccountId(), 11);
        writeFixedString(dos, account.getActiveStatus(), 1);
        
        writePackedDecimal(dos, account.getCurrentBalance(), 7);
        writeFixedString(dos, formatMonetary(account.getCreditLimit()), 13);
        writePackedDecimal(dos, account.getCashCreditLimit(), 7);
        
        writeFixedString(dos, account.getFormattedOpenDate(), 10);
        writeFixedString(dos, account.getFormattedExpirationDate(), 10);
        writeFixedString(dos, account.getFormattedReissueDate(), 10);
        
        writeFixedString(dos, formatMonetary(account.getCurrentCycleCredit()), 13);
        dos.writeLong(account.getCurrentCycleDebit().movePointRight(2).longValue());
        
        writeFixedString(dos, account.getZipCode(), 10);
        writeFixedString(dos, account.getGroupId(), 10);
    }
    
    private void writeTransactionData(DataOutputStream dos, TransactionExportData transaction) throws IOException {
        writeFixedString(dos, transaction.getTransactionId(), 16);
        writeFixedString(dos, transaction.getTransactionTypeCode(), 2);
        writeFixedString(dos, String.format("%04d", 
                transaction.getTransactionCategoryCode() != null ? transaction.getTransactionCategoryCode() : 0), 4);
        writeFixedString(dos, transaction.getTransactionSource(), 10);
        writeFixedString(dos, transaction.getDescription(), 100);
        
        writePackedDecimal(dos, transaction.getAmount(), 6);
        dos.writeInt(transaction.getMerchantId() != null ? transaction.getMerchantId().intValue() : 0);
        
        writeFixedString(dos, transaction.getMerchantName(), 50);
        writeFixedString(dos, transaction.getMerchantCity(), 50);
        writeFixedString(dos, transaction.getMerchantZipCode(), 10);
        writeFixedString(dos, transaction.getCardNumber(), 16);
        
        writeFixedString(dos, transaction.getFormattedOriginationTimestamp(), 26);
        writeFixedString(dos, transaction.getFormattedProcessingTimestamp(), 26);
    }
    
    private void writeCardXrefData(DataOutputStream dos, CardXrefExportData cardXref) throws IOException {
        writeFixedString(dos, cardXref.getCardNumber(), 16);
        writeFixedString(dos, cardXref.getCustomerId(), 9);
        dos.writeLong(cardXref.getAccountId());
    }
    
    private void writeCardData(DataOutputStream dos, CardExportData card) throws IOException {
        writeFixedString(dos, card.getCardNumber(), 16);
        dos.writeLong(card.getAccountId());
        dos.writeShort(card.getCvvCode() != null ? card.getCvvCode() : 0);
        writeFixedString(dos, card.getEmbossedName(), 50);
        writeFixedString(dos, card.getFormattedExpirationDate(), 10);
        writeFixedString(dos, card.getActiveStatus(), 1);
    }
    
    private ExportRecordData readData(DataInputStream dis, RecordType recordType) throws IOException {
        return switch (recordType) {
            case CUSTOMER -> readCustomerData(dis);
            case ACCOUNT -> readAccountData(dis);
            case CARD_XREF -> readCardXrefData(dis);
            case TRANSACTION -> readTransactionData(dis);
            case CARD -> readCardData(dis);
        };
    }
    
    private CustomerExportData readCustomerData(DataInputStream dis) throws IOException {
        long customerId = Integer.toUnsignedLong(dis.readInt());
        String firstName = readFixedString(dis, 25);
        String middleName = readFixedString(dis, 25);
        String lastName = readFixedString(dis, 25);
        
        List<String> addressLines = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            addressLines.add(readFixedString(dis, 50));
        }
        
        String stateCode = readFixedString(dis, 2);
        String countryCode = readFixedString(dis, 3);
        String zipCode = readFixedString(dis, 10);
        
        List<String> phoneNumbers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            phoneNumbers.add(readFixedString(dis, 15));
        }
        
        String ssn = readFixedString(dis, 9);
        String governmentIssuedId = readFixedString(dis, 20);
        String dobStr = readFixedString(dis, 10);
        String eftAccountId = readFixedString(dis, 10);
        String primaryCardHolderInd = readFixedString(dis, 1);
        
        int ficoScore = readPackedDecimalAsInt(dis, 2);
        
        dis.skipBytes(134);
        
        CustomerExportData.Builder builder = CustomerExportData.builder()
                .customerId(customerId)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .addressLines(addressLines)
                .stateCode(stateCode)
                .countryCode(countryCode)
                .zipCode(zipCode)
                .phoneNumbers(phoneNumbers)
                .ssn(ssn)
                .governmentIssuedId(governmentIssuedId)
                .eftAccountId(eftAccountId)
                .primaryCardHolderIndicator(primaryCardHolderInd)
                .ficoCreditScore(ficoScore);
        
        if (!dobStr.isBlank()) {
            try {
                builder.dateOfBirth(LocalDate.parse(dobStr.trim(), DATE_FORMATTER));
            } catch (Exception ignored) {}
        }
        
        return builder.build();
    }
    
    private AccountExportData readAccountData(DataInputStream dis) throws IOException {
        String accountId = readFixedString(dis, 11);
        String activeStatus = readFixedString(dis, 1);
        
        BigDecimal currentBalance = readPackedDecimalAsBigDecimal(dis, 7, 2);
        BigDecimal creditLimit = parseMonetary(readFixedString(dis, 13));
        BigDecimal cashCreditLimit = readPackedDecimalAsBigDecimal(dis, 7, 2);
        
        String openDateStr = readFixedString(dis, 10);
        String expirationDateStr = readFixedString(dis, 10);
        String reissueDateStr = readFixedString(dis, 10);
        
        BigDecimal currentCycleCredit = parseMonetary(readFixedString(dis, 13));
        BigDecimal currentCycleDebit = BigDecimal.valueOf(dis.readLong()).movePointLeft(2);
        
        String zipCode = readFixedString(dis, 10);
        String groupId = readFixedString(dis, 10);
        
        dis.skipBytes(352);
        
        AccountExportData.Builder builder = AccountExportData.builder()
                .accountId(accountId)
                .activeStatus(activeStatus)
                .currentBalance(currentBalance)
                .creditLimit(creditLimit)
                .cashCreditLimit(cashCreditLimit)
                .currentCycleCredit(currentCycleCredit)
                .currentCycleDebit(currentCycleDebit)
                .zipCode(zipCode)
                .groupId(groupId);
        
        if (!openDateStr.isBlank()) {
            try {
                builder.openDate(LocalDate.parse(openDateStr.trim(), DATE_FORMATTER));
            } catch (Exception ignored) {}
        }
        if (!expirationDateStr.isBlank()) {
            try {
                builder.expirationDate(LocalDate.parse(expirationDateStr.trim(), DATE_FORMATTER));
            } catch (Exception ignored) {}
        }
        if (!reissueDateStr.isBlank()) {
            try {
                builder.reissueDate(LocalDate.parse(reissueDateStr.trim(), DATE_FORMATTER));
            } catch (Exception ignored) {}
        }
        
        return builder.build();
    }
    
    private TransactionExportData readTransactionData(DataInputStream dis) throws IOException {
        String transactionId = readFixedString(dis, 16);
        String transactionTypeCode = readFixedString(dis, 2);
        String categoryCodeStr = readFixedString(dis, 4);
        String transactionSource = readFixedString(dis, 10);
        String description = readFixedString(dis, 100);
        
        BigDecimal amount = readPackedDecimalAsBigDecimal(dis, 6, 2);
        long merchantId = Integer.toUnsignedLong(dis.readInt());
        
        String merchantName = readFixedString(dis, 50);
        String merchantCity = readFixedString(dis, 50);
        String merchantZipCode = readFixedString(dis, 10);
        String cardNumber = readFixedString(dis, 16);
        
        String originationTsStr = readFixedString(dis, 26);
        String processingTsStr = readFixedString(dis, 26);
        
        dis.skipBytes(140);
        
        TransactionExportData.Builder builder = TransactionExportData.builder()
                .transactionId(transactionId)
                .transactionTypeCode(transactionTypeCode)
                .transactionSource(transactionSource)
                .description(description)
                .amount(amount)
                .merchantId(merchantId)
                .merchantName(merchantName)
                .merchantCity(merchantCity)
                .merchantZipCode(merchantZipCode)
                .cardNumber(cardNumber);
        
        if (!categoryCodeStr.isBlank()) {
            try {
                builder.transactionCategoryCode(Integer.parseInt(categoryCodeStr.trim()));
            } catch (Exception ignored) {}
        }
        
        if (!originationTsStr.isBlank()) {
            try {
                builder.originationTimestamp(parseTimestamp(originationTsStr));
            } catch (Exception ignored) {}
        }
        
        if (!processingTsStr.isBlank()) {
            try {
                builder.processingTimestamp(parseTimestamp(processingTsStr));
            } catch (Exception ignored) {}
        }
        
        return builder.build();
    }
    
    private CardXrefExportData readCardXrefData(DataInputStream dis) throws IOException {
        String cardNumber = readFixedString(dis, 16);
        String customerId = readFixedString(dis, 9);
        long accountId = dis.readLong();
        
        dis.skipBytes(427);
        
        return CardXrefExportData.builder()
                .cardNumber(cardNumber)
                .customerId(customerId)
                .accountId(accountId)
                .build();
    }
    
    private CardExportData readCardData(DataInputStream dis) throws IOException {
        String cardNumber = readFixedString(dis, 16);
        long accountId = dis.readLong();
        int cvvCode = dis.readShort() & 0xFFFF;
        String embossedName = readFixedString(dis, 50);
        String expirationDateStr = readFixedString(dis, 10);
        String activeStatus = readFixedString(dis, 1);
        
        dis.skipBytes(373);
        
        CardExportData.Builder builder = CardExportData.builder()
                .cardNumber(cardNumber)
                .accountId(accountId)
                .cvvCode(cvvCode)
                .embossedName(embossedName)
                .activeStatus(activeStatus);
        
        if (!expirationDateStr.isBlank()) {
            try {
                builder.expirationDate(LocalDate.parse(expirationDateStr.trim(), DATE_FORMATTER));
            } catch (Exception ignored) {}
        }
        
        return builder.build();
    }
    
    private void writeFixedString(DataOutputStream dos, String value, int length) throws IOException {
        String padded = value != null ? value : "";
        if (padded.length() > length) {
            padded = padded.substring(0, length);
        }
        byte[] bytes = padded.getBytes(charset);
        dos.write(bytes);
        
        int padding = length - bytes.length;
        for (int i = 0; i < padding; i++) {
            dos.writeByte(' ');
        }
    }
    
    private String readFixedString(DataInputStream dis, int length) throws IOException {
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, charset).trim();
    }
    
    private void writePackedDecimal(DataOutputStream dos, Number value, int byteLength) throws IOException {
        long longValue = value != null ? value.longValue() : 0;
        byte[] bytes = new byte[byteLength];
        
        for (int i = byteLength - 1; i >= 0; i--) {
            bytes[i] = (byte) (longValue & 0xFF);
            longValue >>= 8;
        }
        
        dos.write(bytes);
    }
    
    private void writePackedDecimal(DataOutputStream dos, BigDecimal value, int byteLength) throws IOException {
        long longValue = value != null ? value.movePointRight(2).longValue() : 0;
        byte[] bytes = new byte[byteLength];
        
        for (int i = byteLength - 1; i >= 0; i--) {
            bytes[i] = (byte) (longValue & 0xFF);
            longValue >>= 8;
        }
        
        dos.write(bytes);
    }
    
    private int readPackedDecimalAsInt(DataInputStream dis, int byteLength) throws IOException {
        byte[] bytes = new byte[byteLength];
        dis.readFully(bytes);
        
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }
    
    private BigDecimal readPackedDecimalAsBigDecimal(DataInputStream dis, int byteLength, int scale) throws IOException {
        byte[] bytes = new byte[byteLength];
        dis.readFully(bytes);
        
        long result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return BigDecimal.valueOf(result).movePointLeft(scale);
    }
    
    private String formatTimestamp(Instant instant) {
        if (instant == null) {
            return "";
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return TIMESTAMP_FORMATTER.format(ldt);
    }
    
    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) {
            return Instant.now();
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(timestampStr.trim(), TIMESTAMP_FORMATTER);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return Instant.now();
        }
    }
    
    private String formatMonetary(BigDecimal value) {
        if (value == null) {
            return "0000000000000";
        }
        long cents = value.movePointRight(2).longValue();
        return String.format("%+013d", cents);
    }
    
    private BigDecimal parseMonetary(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            long cents = Long.parseLong(value.trim());
            return BigDecimal.valueOf(cents).movePointLeft(2);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
