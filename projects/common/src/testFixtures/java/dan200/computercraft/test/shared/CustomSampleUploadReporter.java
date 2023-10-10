// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;
import dan200.computercraft.shared.computer.upload.FileUpload;
import net.jqwik.api.SampleReportingFormat;

/**
 * Custom jqwik formatters for some of our internal types.
 */
@AutoService(SampleReportingFormat.class)
public class CustomSampleUploadReporter implements SampleReportingFormat {
    @Override
    public boolean appliesTo(Object value) {
        return value instanceof FileUpload;
    }

    @Override
    public Object report(Object value) {
        if (value instanceof FileUpload upload) {
            return String.format("FileUpload(name=%s, contents=%s)", upload.getName(), upload.getBytes());
        } else {
            throw new IllegalStateException("Unexpected value  " + value);
        }
    }
}
