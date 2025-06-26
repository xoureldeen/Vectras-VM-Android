package com.vectras.vm.utils;

import android.app.Activity;
import android.util.Log;

import java.util.Objects;

public class ZipUtils {

    public static int lastProgress = 0;
    public static double lastZipFileSize = 0;

    public static void reset() {
        lastProgress = 0;
        lastZipFileSize = 0;
    }

    public static int getCompressionProgress(double _allfilesize, double _zipfilesize) {
        int _result = 0;
        double allfilesizeMB = _allfilesize / (1024 * 1024);
        double maximumPredictedSize = allfilesizeMB / 2;
        double zipFileSizeMB = _zipfilesize / (1024 * 1024);
        double currentProgress = (zipFileSizeMB / maximumPredictedSize) * 100;
        if (currentProgress < 0) {
//            if ((int)currentProgress != lastProgress) {
                _result = lastProgress + 1 ;
//            }
        } else {
            _result = (int) currentProgress;
        }
        if (_result > 99) {
            _result = 99;
        }
        lastProgress = _result;
        return _result;
    }

    public static int getRemainingCompressionTime(double _allfilesize, double _zipfilesize) {
        int _result = 0;
        double allfilesizeMB = _allfilesize / (1024 * 1024);
        double zipFileSizeMB = _zipfilesize / (1024 * 1024);
        double maximumPredictedSize = allfilesizeMB / 2;
        if (lastProgress > 0) {
            _result = (int) ((maximumPredictedSize - zipFileSizeMB) / (zipFileSizeMB - lastZipFileSize));
        } else {
            _result = 3600;
        }
        if (_result < 0) {
            _result = _result * -1;
        }
        lastZipFileSize = zipFileSizeMB;
        return _result ;
    }

    public static int getDecompressionProgress(double _unpackedsize, double _zipfilesize) {
        int _result = 0;
        double unpackedsizeMB = _unpackedsize / (1024 * 1024);
        double zipFileSizeMB = _zipfilesize / (1024 * 1024);
        double maximumPredictedSize = zipFileSizeMB * 2.5;
        double currentProgress = (unpackedsizeMB / maximumPredictedSize) * 100;
        if (currentProgress < 0) {
//            if ((int)currentProgress != lastProgress) {
                _result = lastProgress + 1 ;
//            }
        } else {
            _result = (int) currentProgress;
        }
        if (_result > 99) {
            _result = 99;
        }
        lastProgress = _result;
        return _result;
    }

    public static int getRemainingDecompressionTime(double _unpackedsize, double _zipfilesize) {
        int _result = 0;
        double unpackedsizeMB = _unpackedsize / (1024 * 1024);
        double zipFileSizeMB = _zipfilesize / (1024 * 1024);
        double maximumPredictedSize = zipFileSizeMB * 2.5;
        if (lastProgress > 0) {
            _result = (int) ((maximumPredictedSize - unpackedsizeMB) / (unpackedsizeMB - lastZipFileSize));
        } else {
            _result = 3600;
        }
        if (_result < 0) {
            _result = _result * -1;
        }
        lastZipFileSize = unpackedsizeMB;
        return _result ;
    }
}
