import numpy as np
from scipy import signal


def pan_tompkins(ecg_raw, fs = 250):

    # filter ecg_raw with bandpass butter filter
    lowcut = 0.5 * 2 / fs # low cutoff frequency 0.5Hz
    highcut = 20 * 2 / fs # high cutoff frequency 20Hz
    sos = signal.butter(5, [lowcut, highcut], 'bandpass', analog = False, output = 'sos')
    ecg_filter = signal.sosfiltfilt(sos, np.array(ecg_raw))

    # Derivaive, get QRS slop information
    ecg_diff = np.ediff1d(ecg_filter)


    # Amplify the R peak, restrict false positive caused by T wave
    ecg_square = ecg_diff ** 2

    # Moving-window integration
    integration_window = int(0.12 * fs) #should change according to diffferent sampling frequency
    ecg_integrate = np.convolve(ecg_square, np.ones(integration_window))

    # Peak detection on ecg_integrate
    ecg_integrate_peaks = panPeakDetect(ecg_integrate, fs)
    rpeaks = searchBack(ecg_integrate_peaks, np.array(ecg_raw), integration_window)


    return rpeaks


def searchBack(detected_peaks, unfiltered_ecg, search_samples):

    r_peaks = []
    window = search_samples

    for i in detected_peaks:
        if i<window:
            section = unfiltered_ecg[:i]
            r_peaks.append(np.argmax(section))
        else:
            section = unfiltered_ecg[i-window:i]
            r_peaks.append(np.argmax(section)+i-window)

    return np.array(r_peaks)



def panPeakDetect(detection, fs):

    min_distance = int(0.15*fs)
    peaks, _ = signal.find_peaks(detection, distance=min_distance)

    signal_peaks = []
    noise_peaks = []

    SPKI = 0.0
    NPKI = 0.0

    threshold_I1 = 0.0
    threshold_I2 = 0.0

    RR_missed = 0
    index = 0
    indexes = []

    missed_peaks = []
    for peak in peaks:

        if detection[peak] > threshold_I1:

            signal_peaks.append(peak)
            indexes.append(index)
            SPKI = 0.125*detection[signal_peaks[-1]] + 0.875*SPKI
            if RR_missed!=0:
                if signal_peaks[-1]-signal_peaks[-2]>RR_missed:
                    missed_section_peaks = peaks[indexes[-2]+1:indexes[-1]]
                    missed_section_peaks2 = []
                    for missed_peak in missed_section_peaks:
                        if missed_peak-signal_peaks[-2]>min_distance and signal_peaks[-1]-missed_peak>min_distance and detection[missed_peak]>threshold_I2:
                            missed_section_peaks2.append(missed_peak)

                    if len(missed_section_peaks2)>0:
                        missed_peak = missed_section_peaks2[np.argmax(detection[missed_section_peaks2])]
                        missed_peaks.append(missed_peak)
                        signal_peaks.append(signal_peaks[-1])
                        signal_peaks[-2] = missed_peak

        else:
            noise_peaks.append(peak)
            NPKI = 0.125*detection[noise_peaks[-1]] + 0.875*NPKI

        threshold_I1 = NPKI + 0.25*(SPKI-NPKI)
        threshold_I2 = 0.5*threshold_I1

        if len(signal_peaks)>8:
            RR = np.diff(signal_peaks[-9:])
            RR_ave = int(np.mean(RR))
            RR_missed = int(1.66*RR_ave)

        index = index+1

    signal_peaks.pop(0)
    return signal_peaks