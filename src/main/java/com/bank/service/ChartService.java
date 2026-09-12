package com.bank.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ChartService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Vẽ line chart với nhiều series, mỗi series là Map<ngày, giá trị> đã sắp theo thời gian.
     * seriesData: key = tên series (VD "Tổng giao dịch", "Số dư", "Phí"), value = TreeMap<ngày, giá trị>
     */
    public byte[] renderLineChart(String title, Map<String, TreeMap<java.time.LocalDate, Double>> seriesData)
            throws IOException {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, TreeMap<java.time.LocalDate, Double>> series : seriesData.entrySet()) {
            for (Map.Entry<java.time.LocalDate, Double> point : series.getValue().entrySet()) {
                dataset.addValue(point.getValue(), series.getKey(), point.getKey().format(DAY_FORMAT));
            }
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title, "Ngày", "Giá trị (VND)", dataset,
                PlotOrientation.VERTICAL, true, true, false);

        BufferedImage image = chart.createBufferedImage(800, 400);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}