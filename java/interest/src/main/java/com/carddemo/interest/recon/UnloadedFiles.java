package com.carddemo.interest.recon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.DecodedExtract;
import com.carddemo.interest.load.ExtractDecoder;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractProfile;
import com.carddemo.interest.load.RejectedRecord;

/**
 * The file side of RC1 to RC4: the four unloaded files of one input directory, decoded with the
 * loader's decoder (the encoding rules stay in the codec, standards S3.5). A file that is missing
 * or has a record on the reject list (TL3) gives no figure.
 */
public final class UnloadedFiles {

    private final Path directory;
    private final RecordFormat format;
    private final Map<ExtractFile, DecodedExtract> extracts = new EnumMap<>(ExtractFile.class);
    private final Map<ExtractFile, String> whyNot = new EnumMap<>(ExtractFile.class);
    private final List<RejectedRecord> rejects = new ArrayList<>();

    private UnloadedFiles(Path directory, RecordFormat format) {
        this.directory = directory;
        this.format = format;
    }

    public static UnloadedFiles read(Path directory, RecordFormat format) {
        UnloadedFiles files = new UnloadedFiles(directory, format);
        ExtractDecoder decoder = new ExtractDecoder(format);
        for (ExtractFile file : ExtractFile.values()) {
            Path path = directory.resolve(file.fileName());
            if (!Files.isRegularFile(path)) {
                files.whyNot.put(file, "no file " + file.fileName());
                continue;
            }
            try {
                int rejectsBefore = files.rejects.size();
                Optional<DecodedExtract> decoded = decoder.decode(file, Files.readAllBytes(path), files.rejects);
                if (decoded.isPresent()) {
                    files.extracts.put(file, decoded.get());
                } else {
                    files.whyNot.put(file, (files.rejects.size() - rejectsBefore) + " record(s) of " + file.fileName()
                            + " on the reject list");
                }
            } catch (IOException e) {
                files.whyNot.put(file, "cannot read " + file.fileName());
            }
        }
        return files;
    }

    public Path directory() {
        return directory;
    }

    public RecordFormat format() {
        return format;
    }

    public Optional<DecodedExtract> extract(ExtractFile file) {
        return Optional.ofNullable(extracts.get(file));
    }

    /** TL3: file, record number, field and reason; never a value. */
    public List<RejectedRecord> rejects() {
        return List.copyOf(rejects);
    }

    public Figure count(ExtractFile file) {
        return extract(file).map(extract -> Figure.of(extract.figures().count())).orElseGet(() -> notAvailable(file));
    }

    public Figure sum(ExtractFile file, String column) {
        return extract(file).map(extract -> Figure.of(extract.figures().sum(column))).orElseGet(() -> notAvailable(file));
    }

    /** RC4: the key of the first (or last) record of the file. */
    public Figure edgeKey(ExtractFile file, boolean isLast) {
        return extract(file).map(extract -> extract.rows().isEmpty()
                ? Figure.notAvailable("the file has no record")
                : Figure.of(keyText(keyOf(file, extract.rows().get(isLast ? extract.rows().size() - 1 : 0)))))
                .orElseGet(() -> notAvailable(file));
    }

    /** TL4, from the files; empty when one of the four files gives no figures. */
    public Optional<ExtractProfile> profile() {
        return extracts.size() == ExtractFile.values().length
                ? Optional.of(ExtractProfile.of(extracts, format.codePage().space())) : Optional.empty();
    }

    public Figure notAvailable(ExtractFile file) {
        return Figure.notAvailable(whyNot.getOrDefault(file, "no file " + file.fileName()));
    }

    /** RC2: the columns that are summed: every amount of the file and the account id that is its hash total. */
    static List<String> summedColumns(ExtractFile file) {
        return file.layout().fields().stream()
                .filter(field -> field.kind() == Kind.SIGNED || field.name().equals(file.hashTotalField()))
                .map(file::column).toList();
    }

    static List<Object> keyOf(ExtractFile file, RecordValues values) {
        List<Object> key = new ArrayList<>();
        for (String name : file.keyFields()) {
            FieldLayout field = file.layout().field(name);
            key.add(field.kind() == Kind.UNSIGNED ? (Object) values.unsigned(name) : values.text(name));
        }
        return key;
    }

    static String keyText(List<Object> key) {
        return String.join("/", key.stream().map(String::valueOf).toList());
    }
}
