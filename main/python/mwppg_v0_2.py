#!/usr/bin/env python
# coding: utf-8

# # Code

# In[1]:


# The test data was recorded under 250Hz, 1min each segment, totally 8 segments
#   Five columns: BluePPG, InfraredPPG, GreenPPG, YellowPPG, ECG
# Mercury BP was recorded at the end of each segment, 5 SBP/DBP for calibraion, 3 SBP/DBP for estimation
# Pick the middle 20sec of each segment for feactures exctration and BP estimation
#   NOTE: it should be a moving window to select the best 20sec data from the 1min data for precessing
# Refer to the MATLAB code in folder "mwppg_bp_v1_190610_demo" written by Shirong Qiu for original algorithm
# version 0.1
# Ningqi Luo, 2020/2/18-2020/2/28


# ## MWPPG Model, all functions

# In[2]:


############ THE PPG FILTER AFFECT THE DIFFPPG SIGNAL #############
# to smooth the ppg signal, 
################# Shirong's matlab code: [ppgF] = ppgFilter0534Hz
################# fL = 0.5, fH = 3.4 
#################   WHY  ######################
import numpy as np
import heartpy as hp
from statistics import *
from scipy import signal
from scipy.stats.mstats import mquantiles
from scipy.interpolate import interp1d


## mwppg model features
ftype = np.dtype({'names':['g_s2v_tt','g_hr','mw_dv2is_tt'],'formats':['f','f','f']})  #features data type
bptype = np.dtype({'names':['SBP','DBP','MBP'],'formats':['f','f','f']}) #BP data type


def PPGFilterMWPPG(ppg_raw, frq=250): 
    order = 4  # filter order of 4*2 = 8
    stpa = 20  # 40 dB of stopband attenuation, amplitude decrease to 1%
    fL = 0.5  # lower stopband frequency
    fH = 5  # higher stopband frequency
    frq = frq/2
    nfL = fL/frq
    nfH = fH/frq
    b, a = signal.cheby2(order, stpa, [nfL, nfH], 'bandpass', analog=False)
    ppg_filter = signal.filtfilt(b, a, ppg_raw)
    return ppg_filter

# Shirong's model DID NOT implement LiuJing's MWPPG model
# but just use a fix p,q value
# updated on 2020/2/23

def diff_mwppg(bppgr,yppgr,ippgr,frq=250):
    q = 0.1
    p = 0.1
    shifting = 100
    
    bppgf = PPGFilterMWPPG(bppgr,frq) #filter data, AC component
    yppgf = PPGFilterMWPPG(yppgr,frq)
    ippgf = PPGFilterMWPPG(ippgr,frq)
    
    bppg0 = bppgr - bppgf #DC component = raw-filter, the baseline
    yppg0 = yppgr - yppgf
    ippg0 = ippgr - ippgf
 
    # ############# fix p,q value #############################################
    # ############# should update to MWPPG model ##############################
    diffppg = (ippgr/ippg0)/(((yppgr/yppg0)**p)*((bppgr/bppg0)**q))
    
    # rescale diffppg to (0,1024), not neccessary
    upper = 1024
    lower = 0
    rng = np.max(diffppg) - np.min(diffppg)
    minimum = np.min(diffppg)
    diffppg = (upper - lower) * ((diffppg - minimum) / rng) + lower #heartpy normalize

    return diffppg


# find valid peaks, use heartpy function

def findpeaks_ppg(data, frq = 250.0, figplot = False):
    '''
    Use heartpy.process function to find peaks and check validity
    
    wd['peaklist'], contain all peaks index found
    wd['ybeat'], peaks value (y) in the waveform
    wd['removed_beats'], peaks supposed invalid
        refer to heartpy.peakdetection.check_peaks() for criteria
    wd['removed_beats_y'], peaks value of those invalid peak
    wd['binary_peaklist'], 1 indicate valid peak, 0 indicate invalid peak
    
    Parameters
    ----------
    data: (N,), array_like
        filtered PPG waveform
    frq: int or float
        sample_rate
    figplot: bool
        True: plot all the figure during data process
        False: default, silence 
    
    Return
    -------
    selectpeaki: (N,), array_like
        peak index, selected from the peak list
    
    '''
    data_scale = hp.scale_data(data, lower=0, upper=1024) #rescale the in range [0,1024], 10bit
    wd, m = hp.process(data_scale, sample_rate = frq) # wd is working_data (a dict)
    
    selectpeaki = [] # select the valid peaks from wd['peaklist']
    for i,v in enumerate(wd['binary_peaklist']): # i: index, v: value
        if v == 1:
            selectpeaki.append(wd['peaklist'][i])
    if figplot == True:
        findpeaks_plot(data_scale,wd,m,frq) # plot all peaks and valid peaks
    return np.array(selectpeaki)    


def findvalleys_ppg(data, frq = 250.0, figplot = False):
    #upsidedown the data, peak positions are valley
    return findpeaks_ppg(-data, frq, figplot)


def findslops_ppg(data, frq = 250.0, figplot = False):
    # first order differencial, gradient is the max slop
    return findpeaks_ppg(np.gradient(data), frq, figplot)
    

def find_valid_vpi_pairs(valleyi, peaki, frq = 250, dist_sec = 1):
    '''
    Find valid valley-peak pairs
    Criteria: 0 < peaki - valleyi < threshold
    1. loop the peaki, find the closest (valleyi, peaki), where pi - vi > 0
    2. loop the (valleyi, peaki) pairs, delete those pairs: pi - vi > threshold
    
    Parameters
    ----------
    valleyi: (N,) array_like
            valley index
    peaki: (N,) array_like
            peak index
    frq: int, optional
        sampling frequency, default 250Hz
    dist_sec: float/int, optional
        distance of (peaki - valleyi), in unit of second
        if the (pi-vi) larger than dist_sec, consider missing peak/valley(s) in between

    Return
    --------
    v_valleyi: (N,) array_like
        valid valleyi
    v_peaki: (N,) array_like
        valid peaki    
    
    ''' 
    vii = [] #valid valleyi index
    pii = [] #valid peaki index
    
    ### find (vii, pii) pairs, where peaki[pii] - valleyi[vii] > 0, i.e. pv - vv > 0, the closest one
    for pi,pv in enumerate(peaki): # loop peaki, peaki index (pi), peaki value(pv)
        temp = [vi for (vi, vv) in enumerate(valleyi) if pv-vv > 0] # loop valleyi
        if temp: #temp not empty
            # (1). vii empty, save the temp[-1] to vii
            # or (2). vii not empty and current found temp[-1] not equal to previous found vii[-1]
            if not vii or temp[-1] != vii[-1]: 
                vii.append(temp[-1])
                pii.append(pi)
     
    vii = np.array(vii) # list to array
    pii = np.array(pii)

    ### delete those pairs, where peaki - valleyi > threshold
    threshold = frq * dist_sec # if peaki-valleyi distance is larger than threshold, delete that pair 
    
    pv_dist = peaki[pii] - valleyi[vii] # peaki-valleyi distance
    for dii, div in enumerate(pv_dist): # loop the to_be_deleted_index (dii) and value (div)
        if div >= threshold:
            vii = np.delete(vii, dii)
            pii = np.delete(pii, dii)

    ### return valid valleyi and peaki
    return [valleyi[vii],peaki[pii]] 


def features_mwppg(bppgr,gppgr,yppgr,ippgr,frq=250):
    '''
    1. gppg feature, g_s2v_tt, g_hr
    2. mwppg feature, mw_dv2is_tt
    3. return median_feature = [median(g_s2v_tt), median(g_hr), median(mw_dv2is_tt)]
                gtt, ghr, mwtt array
    '''  
    
    ## gppg feature extraction, PTT and HR in shirong's code

    gppgf = PPGFilterMWPPG(gppgr,frq) # filter data, for gppg feature extraction

    g_slopi = findslops_ppg(gppgf,frq) # gppg slop index
    g_valleyi = findvalleys_ppg(gppgf,frq) # gppg valley index
    vg_valleyi, vg_slopi = find_valid_vpi_pairs(g_valleyi, g_slopi,frq) # valid index pairs (g_valleyi, g_slopi)

    g_s2v_tt = (vg_slopi - vg_valleyi) / frq # gppg, slop to valley transit time, unit: sec
    #### BUG ######## should consider missing slops among g_slopi ##################
    #### CONFUSE #### HR is normally use unit bea per minute (BPM), not beat per second ##########
    g_hr = frq / np.diff(g_slopi)  # gppg HR, unit: beat pre second 


    ## diffppg and ippg feature extration

    ippgf = PPGFilterMWPPG(ippgr,frq) # ippg filter data
    diffppg = diff_mwppg(bppgr,yppgr,ippgr) # MWPPG model

    i_slopi = findslops_ppg(ippgf,frq) # ippg slop index
    d_valleyi = findvalleys_ppg(diffppg,frq) # diffppg valley index
    #### Intentionally: find the POSITIVE tt between diffppg valley and ippg slop ####
    #### i.e., diffppg_valley[i+1] - ippg_slop[i]
    # valid index pairs (i_slopi, d_valleyi)
    vi_slopi, vd_valleyi = find_valid_vpi_pairs(i_slopi, d_valleyi, frq, dist_sec = 1.5) 
    #multi-wavelength, diffppg_valley to ippg_slop, transit time, unit: sec
    mw_dv2is_tt = (vd_valleyi - vi_slopi) / frq 
    
    median_feature = np.array((np.median(g_s2v_tt), np.median(g_hr), np.median(mw_dv2is_tt)),dtype = ftype)

    # g_s2v_tt, g_hr, mw_dv2is_tt are in different length, can not combine
    return [median_feature, g_s2v_tt, g_hr, mw_dv2is_tt]



def features_mwppg_est(bppgr,gppgr,yppgr,ippgr, cal_median_ghr, frq=250):
    
    # feature extraction function 
    median_feature, g_s2v_tt, g_hr, mw_dv2is_tt = features_mwppg(bppgr,gppgr,yppgr,ippgr) 
    
    #############################################################################
    ## Shirong's process, for estimation ONLY, gtt shift among [-5,5]/frq
    ## use ghr as index, to shift gtt
    #############################################################################
    iscHP = median_feature['g_hr']/np.mean(cal_median_ghr)    
    scHP = np.linspace(-5,5,25)
    iisc = int(np.fix((iscHP-1)*100)+np.fix(len(scHP)/2))
    if iisc >= len(scHP):
        iisc = len(scHP)-1
    elif iisc <= 0:
        iisc = 0
    scHP_shift = scHP[iisc]/frq
    #############################################################################
    ## use ghr as index, to shift gtt
    median_feature['g_s2v_tt'] = median_feature['g_s2v_tt'] - scHP_shift
    
    # g_s2v_tt, g_hr, mw_dv2is_tt are in different length, can not combine
    return [median_feature, g_s2v_tt, g_hr, mw_dv2is_tt]
    

## MWPPG BP model

def gttPara(cal_gtt,calSBP):
    ### Y value, SBP_range to be interpolated
    SBP_range = (max(calSBP) - min(calSBP))/2
    SBP_2interp = np.linspace(-SBP_range, SBP_range, 7) # devide into 7 data point

    ### X value, gtt_range to be interpolated. devide , then unique 
    gtt_range = (cal_gtt**2)/np.median(cal_gtt**2) 
    quantile_b = np.array([0.125,0.25,0.375,0.5,0.625,0.75,0.875]) # devide point position, 7 points
    gtt_2interp = mquantiles(gtt_range, quantile_b, alphap=0.5, betap=0.5) # the same as matlab fun "quantile"

    ### only the unique gtt and it's corresponding SBP are used for interpolation
    uv, ui = np.unique(gtt_2interp, return_index=True) # unique, return unique value and index
    f = interp1d(gtt_2interp[ui], SBP_2interp[ui], kind='linear',fill_value="extrapolate") # matlab interp1

    gtt_interp = np.linspace(0,max(gtt_range)*1.5,int(max(gtt_range)*1.5/0.005)) # 0.005 per step
    SBP_interp = f(gtt_interp)
    
    ### plot the origianl points and the interpolated points
    plt.figure(figsize=(40,10))
    plt.plot(gtt_2interp, SBP_2interp,'r^',gtt_interp,SBP_interp,'b.')
    
    return [gtt_interp,SBP_interp]


def ghrPara(cal_ghr,calSBP):
    ### Y value, SBP_range to be interpolated
    SBP_range = (max(calSBP) - min(calSBP))/2
    SBP_2interp = np.linspace(-SBP_range, SBP_range, 7) # devide into 7 data point
    
    ### X value, ghr_range to be interpolated. devide , then unique 
    ghr_range = cal_ghr / np.median(cal_ghr)
    quantile_b = np.array([0.125,0.25,0.375,0.5,0.625,0.75,0.875]) # devide point position, 7 points
    ghr_2interp = mquantiles(ghr_range, quantile_b, alphap=0.5, betap=0.5) # the same as matlab fun "quantile"
    
    ### only the unique ghr and it's corresponding SBP are used for interpolation
    uv, ui = np.unique(ghr_2interp, return_index=True) # unique, return unique value and index
    f = interp1d(ghr_2interp[ui], SBP_2interp[ui], kind='linear',fill_value="extrapolate") # matlab interp1

    ghr_interp = np.linspace(0,max(ghr_range)*1.5,int(max(ghr_range)*1.5/0.005)) # 0.005 per step
    SBP_interp = f(ghr_interp)
    
    ### plot the origianl points and the interpolated points
    plt.figure(figsize=(40,10))
    plt.plot(ghr_2interp, SBP_2interp,'r^',ghr_interp,SBP_interp,'b.')
    
    return [ghr_interp, SBP_interp]



def BP_estimation_mwppg(calSBP,calDBP,
                        cal_median,cal_mwtt,cal_ghr,
                        mwtt_interp,mwtt_SBP_interp,ghr_interp,ghr_SBP_interp,
                        est_mwtt,est_ghr):
    '''
    Refer to Shirong's matlab code...
        
    '''   
    
    r_tt = (est_mwtt**2) / (np.median(cal_mwtt)**2)
    r_hr = est_ghr / np.median(cal_ghr)
    
    cal_tthp = cal_median['mw_dv2is_tt']*cal_median['g_hr']
    est_tthp = est_mwtt * est_ghr
    r_tthp = np.median(cal_tthp) / est_tthp
    
    tti = np.argmin(abs(mwtt_interp - r_tt))
    hri = np.argmin(abs(ghr_interp - r_hr))
    
    ## SBP and DBP
    if r_tthp > 1.2:
        rangeSBP = (max(calSBP)-min(calSBP))*0.5
        rangeDBP = (max(calDBP)-min(calDBP))*0.5
    elif r_tthp > 1.02 and r_tthp <=1.2:
        rangeSBP = 0.2*ghr_SBP_interp[hri] + 0.3*mwtt_SBP_interp[tti] + (max(calSBP)-min(calSBP))*0.5
        rangeDBP = 0.2*ghr_SBP_interp[hri] + 0.3*mwtt_SBP_interp[tti] + (max(calDBP)-min(calDBP))*0.5
    elif r_tthp < 0.9:
        rangeSBP = (0.4*ghr_SBP_interp[hri] + 0.6*mwtt_SBP_interp[tti])*0.5
        rangeDBP = (0.5*ghr_SBP_interp[hri] + 0.5*mwtt_SBP_interp[tti])*0.5
    else:
        rangeSBP = 0.6*ghr_SBP_interp[hri] + 0.4*mwtt_SBP_interp[tti]
        rangeDBP = rangeSBP
        
    estSBP = np.mean(calSBP)+rangeSBP
    estDBP = np.mean(calDBP)+rangeDBP*0.5

    scaleSBP = (max(calSBP) - min(calSBP))/2          
    ##SBP
    if r_tthp < 1.02:
        if abs(np.mean(calSBP)-estSBP)>scaleSBP*1.7:
            if np.sign(np.mean(calSBP) - estSBP):
                estSBP = np.mean(calSBP) - scaleSBP
                
    if r_tthp < 1.02:
        if abs(np.mean(calDBP)-estDBP)>scaleSBP*1.7:
            if np.sign(np.mean(calDBP) - estDBP):
                estDBP = np.mean(calDBP) - scaleSBP           
        
    
    estSBP = np.around(estSBP,decimals=1)
    estDBP = np.around(estDBP,decimals=1)
    
    return [estSBP,estDBP]

