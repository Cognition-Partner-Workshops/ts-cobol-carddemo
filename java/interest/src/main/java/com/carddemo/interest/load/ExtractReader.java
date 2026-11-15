package com.carddemo.interest.load;

import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads one unloaded file as record images of the layout's length. Nothing is cut or repaired
 * here: an image of the wrong length is handed on and the codec rejects it (standards S3.6).
 */
final class ExtractReader {

    private final Path inputDir;
    private final ExtractFormat format;

    ExtractReader(Path inputDir, ExtractFormat format) {
        this.inputDir = inputDir;
        this.format = format;
    }

    List<byte[]> read(ExtractFile file) {
        String fileName = file.fileName(format);
        int recordLength = file.layout().length();
        byte[] content;
        try {
            content = Files.readAllBytes(inputDir.resolve(fileName));
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read input file " + fileName, e);
        }
        if (format == ExtractFormat.HOST) {
            return RecordImage.fromBlock(content, recordLength);
        }
        return new String(content, format.codePage().charset()).lines()
                .map(line -> RecordImage.fromLine(line, recordLength, format.codePage()))
                .toList();
    }
}
