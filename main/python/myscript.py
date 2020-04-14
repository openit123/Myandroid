import numpy as np
from scipy import signal
from pan_tompkins import *
import heartpy as hp
from mwppg_v0_2 import *
import os
from os.path import dirname, join
import model

def PPGPeak(ppg_raw):
    data_raw = ppg_raw
    data_scale = hp.scale_data(data_raw, lower=0, upper=1024)
    wd, m = hp.process(data_scale, sample_rate = 250.0)
    return wd['peaklist']

def PPGFilter(ppg_raw, sampling_frequency = 250):
    order = 5  # filter order of 5*2 = 10
    stpa = 40  # 40 dB of stopband attenuation, amplitude decrease to 1%
    fL = 0.5  # lower stopband freq.
    fH = 20  # higher stopband freq.
    fn = sampling_frequency/2
    nfL = fL/fn
    nfH = fH/fn
    b, a = signal.cheby2(order, stpa, [nfL,nfH], 'bandpass', analog=False)
    ppg_raw = signal.filtfilt(b, a, ppg_raw)
    return ppg_raw

def ECGFilter(ecg_raw, sampling_frequency = 250):
    order = 5 # order of the filter
    Fs = 35 # cutoff frequency
    fn = sampling_frequency/2
    nFs = Fs/fn
    b, a = signal.butter(order, nFs, 'lowpass', analog=False)
    ecg_filter = signal.filtfilt(b, a, ecg_raw)
    return ecg_filter

def ECGPeak(ecg_raw):
    rpeak = pan_tompkins(ecg_raw)
    return rpeak


def BPEstimation(bppg,ippg,gppg,yppg,ecg):
    calSBP = np.genfromtxt( join(dirname(__file__),'model/calSBP.csv'), delimiter = ',')
    calDBP = np.genfromtxt(join(dirname(__file__),'model/calDBP.csv'), delimiter = ',')
    cal_median = np.genfromtxt(join(dirname(__file__),'model/cal_median.csv'),names = ['g_s2v_tt','g_hr','mw_dv2is_tt'], delimiter =",")
    cal_gtt = np.genfromtxt(join(dirname(__file__),'model/cal_gtt.csv'), delimiter = ',')
    cal_ghr = np.genfromtxt(join(dirname(__file__),'model/cal_ghr.csv'), delimiter = ',')
    cal_mwtt = np.genfromtxt(join(dirname(__file__),'model/cal_mwtt.csv'), delimiter = ',')
    gtt_interp = np.genfromtxt(join(dirname(__file__),'model/gtt_interp.csv'), delimiter = ',')
    gtt_SBP_interp = np.genfromtxt(join(dirname(__file__),'model/gtt_SBP_interp.csv'), delimiter = ",")
    mwtt_interp = np.genfromtxt(join(dirname(__file__),'model/mwtt_interp.csv'), delimiter = ',')
    mwtt_SBP_interp = np.genfromtxt(join(dirname(__file__),'model/mwtt_SBP_interp.csv'), delimiter = ",")
    ghr_interp = np.genfromtxt(join(dirname(__file__),'model/ghr_interp.csv'), delimiter = ',')
    ghr_SBP_interp = np.genfromtxt(join(dirname(__file__),'model/ghr_SBP_interp.csv'), delimiter = ",")
    segments_for_estimation = [5,6,7]
    frq = 250
    bgn = 20*frq
    end = 40*frq
    est_median = np.array([],dtype=ftype)
    median_feature, _, _, _ = features_mwppg_est(bppg,gppg,yppg,ippg,cal_median['g_hr'])
    est_median = np.append(est_median, median_feature)
    cal_gtthp = cal_median['g_s2v_tt']*cal_median['g_hr']
    est_gtts = est_median['g_s2v_tt']
    est_ghrs = est_median['g_hr']
    est_mwtts = est_median['mw_dv2is_tt']
    estSBP = np.array([])
    estDBP = np.array([])
    SBP, DBP = BP_estimation_mwppg(calSBP,calDBP,cal_median,cal_mwtt,cal_ghr,mwtt_interp,mwtt_SBP_interp,ghr_interp,ghr_SBP_interp,est_mwtts,est_ghrs)
    stSBP = np.append(estSBP,SBP)
    estDBP = np.append(estDBP,DBP)
    estMBP = np.around(1/3*estSBP + 2/3*estDBP, decimals = 1)
    return SBP,DBP
