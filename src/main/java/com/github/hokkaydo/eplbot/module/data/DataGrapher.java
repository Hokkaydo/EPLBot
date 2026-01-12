package com.github.hokkaydo.eplbot.module.data;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DataGrapher {

    /**
     * Generates a line chart for user activity
     * @param userData map of user names to message counts
     * @param title the title of the chart
     * @return byte array of the PNG image
     */
    public static byte[] generateUserActivityChart(Map<String, Long> userData, String title) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<String, Long> entry : userData.entrySet()) {
            dataset.addValue(entry.getValue(), "Messages", entry.getKey());
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
            title,
            "User",
            "Message Count",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );
        
        customizeChart(chart);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(outputStream, chart, 800, 600);
        return outputStream.toByteArray();
    }

    /**
     * Generates a line chart for channel activity
     * @param channelData map of channel names to message counts
     * @param title the title of the chart
     * @return byte array of the PNG image
     */
    public static byte[] generateChannelActivityChart(Map<String, Long> channelData, String title) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<String, Long> entry : channelData.entrySet()) {
            dataset.addValue(entry.getValue(), "Messages", entry.getKey());
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
            title,
            "Channel",
            "Message Count",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );
        
        customizeChart(chart);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(outputStream, chart, 800, 600);
        return outputStream.toByteArray();
    }

    /**
     * Generates a time series chart for member joins/leaves
     * @param joins list of join timestamps
     * @param leaves list of leave timestamps
     * @param title the title of the chart
     * @return byte array of the PNG image
     */
    public static byte[] generateMemberEventsChart(List<Long> joins, List<Long> leaves, String title) throws IOException {
        TimeSeries joinSeries = new TimeSeries("Joins");
        TimeSeries leaveSeries = new TimeSeries("Leaves");
        
        // Count joins and leaves by day
        for (Long timestamp : joins) {
            Date date = Date.from(Instant.ofEpochSecond(timestamp));
            Day day = new Day(date);
            joinSeries.addOrUpdate(day, joinSeries.getValue(day) == null ? 1 : joinSeries.getValue(day).intValue() + 1);
        }
        
        for (Long timestamp : leaves) {
            Date date = Date.from(Instant.ofEpochSecond(timestamp));
            Day day = new Day(date);
            leaveSeries.addOrUpdate(day, leaveSeries.getValue(day) == null ? 1 : leaveSeries.getValue(day).intValue() + 1);
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(joinSeries);
        dataset.addSeries(leaveSeries);
        
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Date",
            "Count",
            dataset,
            true,
            true,
            false
        );
        
        customizeChart(chart);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(outputStream, chart, 800, 600);
        return outputStream.toByteArray();
    }


    private static void customizeChart(JFreeChart chart) {
        Color darkBackground = new Color(45, 45, 48);
        Color plotBackground = new Color(30, 30, 32);
        Color gridColor = new Color(60, 60, 63);
        Color textColor = new Color(230, 230, 230);
        Color barColor = new Color(100, 150, 200);
        
        chart.setBackgroundPaint(darkBackground);
        
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(textColor);
        }
        
        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(plotBackground);
            plot.setDomainGridlinePaint(gridColor);
            plot.setRangeGridlinePaint(gridColor);
            plot.setOutlinePaint(gridColor);
            
            plot.getDomainAxis().setLabelPaint(textColor);
            plot.getDomainAxis().setTickLabelPaint(textColor);
            plot.getRangeAxis().setLabelPaint(textColor);
            plot.getRangeAxis().setTickLabelPaint(textColor);
            
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, barColor);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(0, true);
        } else if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setBackgroundPaint(plotBackground);
            plot.setDomainGridlinePaint(gridColor);
            plot.setRangeGridlinePaint(gridColor);
            plot.setOutlinePaint(gridColor);
            
            plot.getDomainAxis().setLabelPaint(textColor);
            plot.getDomainAxis().setTickLabelPaint(textColor);
            plot.getRangeAxis().setLabelPaint(textColor);
            plot.getRangeAxis().setTickLabelPaint(textColor);
            
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(100, 200, 100));  // Green for joins
            renderer.setSeriesPaint(1, new Color(200, 100, 100));  // Red for leaves
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        }
        
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(darkBackground);
            chart.getLegend().setItemPaint(textColor);
        }
    }

}
