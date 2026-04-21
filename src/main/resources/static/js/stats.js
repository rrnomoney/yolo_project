document.addEventListener("DOMContentLoaded", function () {
    const statsData = document.getElementById("statsData");
    if (!statsData) return;

    const classCountMap = JSON.parse(statsData.dataset.classCountMap || "{}");
    const videoUniqueCountMap = JSON.parse(statsData.dataset.videoUniqueCountMap || "{}");

    const classNames = Object.keys(classCountMap);
    const classValues = Object.values(classCountMap);

    const uniqueNames = Object.keys(videoUniqueCountMap);
    const uniqueValues = Object.values(videoUniqueCountMap);

    let barChart = null;
    let pieChart = null;
    let uniqueChart = null;

    if (classNames.length > 0) {
        barChart = echarts.init(document.getElementById("barChart"));
        pieChart = echarts.init(document.getElementById("pieChart"));

        barChart.setOption({
            tooltip: { trigger: "axis" },
            grid: { left: "8%", right: "5%", bottom: "10%", top: "10%", containLabel: true },
            xAxis: {
                type: "category",
                data: classNames,
                axisLabel: { fontSize: 13 }
            },
            yAxis: {
                type: "value",
                minInterval: 1
            },
            series: [{
                name: "累计次数",
                type: "bar",
                data: classValues,
                barWidth: "45%",
                itemStyle: {
                    borderRadius: [6, 6, 0, 0]
                },
                label: {
                    show: true,
                    position: "top"
                }
            }]
        });

        pieChart.setOption({
            tooltip: { trigger: "item" },
            legend: {
                bottom: 10,
                left: "center"
            },
            series: [{
                name: "目标占比",
                type: "pie",
                radius: ["35%", "65%"],
                center: ["50%", "45%"],
                data: classNames.map((name, index) => ({
                    name: name,
                    value: classValues[index]
                })),
                label: {
                    formatter: "{b}: {d}%"
                }
            }]
        });
    }

    if (uniqueNames.length > 0) {
        uniqueChart = echarts.init(document.getElementById("uniqueChart"));

        uniqueChart.setOption({
            tooltip: { trigger: "axis" },
            grid: { left: "8%", right: "5%", bottom: "10%", top: "10%", containLabel: true },
            xAxis: {
                type: "category",
                data: uniqueNames,
                axisLabel: { fontSize: 13 }
            },
            yAxis: {
                type: "value",
                minInterval: 1
            },
            series: [{
                name: "去重数量",
                type: "bar",
                data: uniqueValues,
                barWidth: "45%",
                itemStyle: {
                    borderRadius: [6, 6, 0, 0]
                },
                label: {
                    show: true,
                    position: "top"
                }
            }]
        });
    }

    window.addEventListener("resize", function () {
        if (barChart) barChart.resize();
        if (pieChart) pieChart.resize();
        if (uniqueChart) uniqueChart.resize();
    });
});