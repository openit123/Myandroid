/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.hrs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Point;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.Collections;

import static android.app.Activity.RESULT_OK;

/**
 * This class uses external library AChartEngine to show dynamic real time line graph for HR values
 */
public class LineGraphView_bp {
    //TimeSeries will hold the data in x,y format for single chart
    int secCount = 0;
    private TimeSeries mSeries_xdim = new TimeSeries("SBP");
    private TimeSeries mSeries_ydim = new TimeSeries("DBP");
    private TimeSeries mSeries_zdim = new TimeSeries("");
    //XYMultipleSeriesDataset will contain all the TimeSeries
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    //XYMultipleSeriesRenderer will contain all XYSeriesRenderer and it can be used to set the properties of whole Graph
    private XYMultipleSeriesRenderer mMultiRenderer = new XYMultipleSeriesRenderer();
    private static LineGraphView_bp mInstance = null;
    ArrayList<Integer> maxmindataset = new ArrayList<Integer>();
    ArrayList<Integer> mindataset = new ArrayList<Integer>();
    int max=Integer.MIN_VALUE; int maxcount=0;
    int min = Integer.MAX_VALUE; int mincount= 0;

    /**
     * singleton implementation of LineGraphView class
     */
    public static synchronized LineGraphView_bp getLineGraphView() {
        if (mInstance == null) {
            mInstance = new LineGraphView_bp();
        }
        return mInstance;
    }

    /**
     * This constructor will set some properties of single chart and some properties of whole graph
     */
    public LineGraphView_bp() {
        //add single line chart mSeries
        mDataset.addSeries(mSeries_xdim);
        mDataset.addSeries(mSeries_ydim);
        mDataset.addSeries(mSeries_zdim);

        //XYSeriesRenderer is used to set the properties like chart color, style of each point, etc. of single chart
        final XYSeriesRenderer seriesRenderer_xdim = new XYSeriesRenderer();
        final XYSeriesRenderer seriesRenderer_ydim = new XYSeriesRenderer();
        final XYSeriesRenderer seriesRenderer_zdim = new XYSeriesRenderer();
        //set line chart color to Black
        seriesRenderer_xdim.setColor(Color.BLACK);
        //set line chart style to square points
        seriesRenderer_xdim.setPointStyle(PointStyle.SQUARE);
        seriesRenderer_xdim.setFillPoints(true);
        //set line chart color to Black
        seriesRenderer_ydim.setColor(Color.BLUE);
        //set line chart style to square points
        seriesRenderer_ydim.setPointStyle(PointStyle.SQUARE);
        seriesRenderer_ydim.setFillPoints(true);
        //set line chart color to Black
        seriesRenderer_zdim.setColor(Color.GREEN);
        //set line chart style to square points
        seriesRenderer_zdim.setPointStyle(PointStyle.SQUARE);
        seriesRenderer_zdim.setFillPoints(true);

        final XYMultipleSeriesRenderer renderer = mMultiRenderer;
        //set whole graph background color to transparent color
        renderer.setBackgroundColor(Color.TRANSPARENT);
        renderer.setMargins(new int[] { 10, 60, 10, 60 }); // top, left, bottom, right
        renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
        renderer.setAxesColor(Color.BLACK);
        renderer.setAxisTitleTextSize(24);
        renderer.setShowGrid(true);
        renderer.setGridColor(Color.LTGRAY);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.DKGRAY);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setYLabelsPadding(4.0f);
        renderer.setXLabelsColor(Color.DKGRAY);
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        //renderer.setYAxisMin(-900);
        //renderer.setYAxisMax(900);
        renderer.setXLabels(RESULT_OK);

        for(int x=0;x<500000;x=x+500){
            String xlab = secCount+" s";
            renderer.addXTextLabel(x,xlab);
            secCount++;
        }
        //Disable zoom
        renderer.setPanEnabled(true, false);
        renderer.setZoomEnabled(true, false);
        //set title to x-axis and y-axis
        renderer.setXTitle("    Time (seconds)");
        renderer.setYTitle("");
        renderer.addSeriesRenderer(seriesRenderer_xdim);
        renderer.addSeriesRenderer(seriesRenderer_ydim);
        renderer.addSeriesRenderer(seriesRenderer_zdim);
    }


    /**
     * return graph view to activity
     */
    public GraphicalView getView(Context context) {
        final GraphicalView graphView = ChartFactory.getLineChartView(context, mDataset, mMultiRenderer);
        return graphView;
    }

    /**
     * add new x,y value to chart
     */
    public void addValue(Point p_x, Point p_y, Point p_z) {
        mSeries_xdim.add(p_x.x, p_x.y);
        mSeries_ydim.add(p_y.x, p_y.y);
        //mSeries_zdim.add(p_z.x, p_z.y);
        int datamax = (int)p_x.y;
        int size = maxmindataset.size();
        if(size<=5000) {
            maxmindataset.add(datamax);
        }else{
            maxmindataset.remove(0);
            maxmindataset.add(datamax);
        }
        //For Max
        if(datamax>=max){
            max = datamax;
            maxcount=0;
        }else{
            maxcount++;
        }
        if(maxcount>=5000){
            //FindMax
            max = Collections.max(maxmindataset);
            maxcount=0;
        }
        int datamin = (int)p_y.y;
        int size2 = mindataset.size();
        if(size2<=5000) {
            mindataset.add(datamin);
        }else{
            mindataset.remove(0);
            mindataset.add(datamin);
        }

        //For Min
        if(datamin<=min){
            min = datamin;
            mincount = 0;
        }else{
            mincount++;
        }
        if(mincount>=5000){
            min = Collections.min(mindataset);
            mincount=0;
        }

        mMultiRenderer.setYAxisMin(min-50);
        mMultiRenderer.setYAxisMax(max+50);

        if(p_x.x<2000){
            mMultiRenderer.setXAxisMin(0);
            mMultiRenderer.setXAxisMax(2000);
        }else{
            mMultiRenderer.setXAxisMin(p_x.x-2000);
            mMultiRenderer.setXAxisMax(p_x.x);}
    }

    /**
     * clear all previous values of chart
     */
    public void clearGraph() {
        mSeries_xdim.clear();
        mSeries_ydim.clear();
        mSeries_zdim.clear();
    }

}


