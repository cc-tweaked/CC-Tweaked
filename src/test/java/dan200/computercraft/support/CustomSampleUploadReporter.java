/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import com.google.auto.service.AutoService;
import dan200.computercraft.shared.computer.upload.FileUpload;
import net.jqwik.api.SampleReportingFormat;

import javax.annotation.Nonnull;

/**
 * Custom jqwik formatters for some of our internal types.
 */
@AutoService(SampleReportingFormat.class)
public class CustomSampleUploadReporter implements SampleReportingFormat {
    @Override
    public boolean appliesTo(@Nonnull Object value) {
        return value instanceof FileUpload;
    }

    @Nonnull
    @Override
    public Object report(@Nonnull Object value) {
        if (value instanceof FileUpload) {
            var upload = (FileUpload) value;
            return String.format("FileUpload(name=%s, contents=%s)", upload.getName(), upload.getBytes());
        } else {
            throw new IllegalStateException("Unexpected value  " + value);
        }
    }
}
