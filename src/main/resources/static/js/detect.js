const form = document.getElementById("uploadForm");
const fileInput = document.getElementById("file");
const submitBtn = document.getElementById("submitBtn");
const loader = document.getElementById("loader");
const imageContainer = document.getElementById("imageContainer");
const originalImage = document.getElementById("originalImage");
const detectedImage = document.getElementById("detectedImage");
const resultDiv = document.getElementById("result");
const tableContainer = document.getElementById("tableContainer");
const summaryContainer = document.getElementById("summaryContainer");
const chartContainer = document.getElementById("chartContainer");
const filterInput = document.getElementById("classFilter");
const filterBtn = document.getElementById("filterBtn");
const resetFilterBtn = document.getElementById("resetFilterBtn");
const detectionResultPre = document.getElementById("detectionResult");

const videoUploadForm = document.getElementById("videoUploadForm");
const videoFileInput = document.getElementById("videoFile");
const videoSubmitBtn = document.getElementById("videoSubmitBtn");
const videoLoader = document.getElementById("videoLoader");
const videoResultCard = document.getElementById("videoResultCard");
const resultVideo = document.getElementById("resultVideo");
const videoInfo = document.getElementById("videoInfo");

let allPredictions = [];
let chartInstance = null;

// 图片预览
fileInput.addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
            originalImage.src = event.target.result;
            imageContainer.style.display = "flex";
            resultDiv.style.display = "none";
            detectedImage.src = event.target.result;
        };
        reader.readAsDataURL(file);
    }
});

// 图片检测
form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(form);

    submitBtn.disabled = true;
    loader.style.display = "block";
    resultDiv.style.display = "none";

    try {
        const response = await fetch("/api/detect/image", {
            method: "POST",
            body: formData,
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `服务器返回状态码 ${response.status}`);
        }

        const result = await response.json();
        console.log("服务器返回原始数据:", result);

        detectionResultPre.innerText = JSON.stringify(result, null, 2);

        let predictions = [];
        if (Array.isArray(result)) {
            predictions = result;
        } else if (result && result.predictions && Array.isArray(result.predictions)) {
            predictions = result.predictions;
        }

        allPredictions = predictions;
        updateResultTable(predictions);

        resultDiv.style.display = "block";
        imageContainer.style.display = "flex";

        if (result.image) {
            detectedImage.src = result.image.startsWith("data:")
                ? result.image
                : `data:image/jpeg;base64,${result.image}`;
        }

    } catch (error) {
        console.error("检测出错:", error);
        alert("检测失败，详细错误: " + error.message);
    } finally {
        submitBtn.disabled = false;
        loader.style.display = "none";
    }
});

function updateResultTable(predictions) {
    if (!predictions || predictions.length === 0) {
        summaryContainer.innerHTML = "";
        if (chartInstance) {
            chartInstance.dispose();
            chartInstance = null;
        }
        chartContainer.innerHTML = "";
        tableContainer.innerHTML = `<p class="empty-text">未检测到任何目标。</p>`;
        return;
    }

    const classCountMap = {};
    predictions.forEach(p => {
        const className = p.name || "unknown";
        classCountMap[className] = (classCountMap[className] || 0) + 1;
    });

    let summaryHtml = `
        <div class="summary-block">
            <div class="summary-title">目标数量统计</div>
            <div class="summary-tags">
    `;

    summaryHtml += `<span class="summary-tag summary-total">总目标：${predictions.length} 个</span>`;

    for (const className in classCountMap) {
        summaryHtml += `<span class="summary-tag">${className}：${classCountMap[className]} 个</span>`;
    }

    summaryHtml += `
            </div>
        </div>
    `;

    summaryContainer.innerHTML = summaryHtml;
    renderChart(classCountMap);

    let html = `
        <div class="table-wrapper">
            <table>
                <thead>
                    <tr>
                        <th>目标名称</th>
                        <th>置信度</th>
                        <th>坐标 (xmin, ymin, xmax, ymax)</th>
                    </tr>
                </thead>
                <tbody>
    `;

    predictions.forEach(p => {
        const confPercent = (p.confidence * 100).toFixed(1) + "%";
        html += `
            <tr>
                <td><strong>${p.name}</strong></td>
                <td>
                    <div class="confidence-bar">
                        <div class="confidence-level" style="width: ${confPercent}"></div>
                    </div>
                    ${confPercent}
                </td>
                <td>${Math.round(p.xmin)}, ${Math.round(p.ymin)}, ${Math.round(p.xmax)}, ${Math.round(p.ymax)}</td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    tableContainer.innerHTML = html;
}

function renderChart(classCountMap) {
    if (chartInstance) {
        chartInstance.dispose();
    }

    chartInstance = echarts.init(chartContainer);

    const classNames = Object.keys(classCountMap);
    const classValues = Object.values(classCountMap);

    const option = {
        title: {
            text: "检测结果类别统计图",
            left: "center",
            top: 10,
            textStyle: {
                fontSize: 16,
                fontWeight: "bold",
                color: "#333"
            }
        },
        tooltip: {
            trigger: "axis"
        },
        grid: {
            left: "8%",
            right: "5%",
            bottom: "10%",
            top: "22%",
            containLabel: true
        },
        xAxis: {
            type: "category",
            data: classNames,
            axisLabel: {
                fontSize: 13
            }
        },
        yAxis: {
            type: "value",
            minInterval: 1
        },
        series: [
            {
                name: "数量",
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
            }
        ]
    };

    chartInstance.setOption(option);
}

window.addEventListener("resize", function () {
    if (chartInstance) {
        chartInstance.resize();
    }
});

// 筛选
filterBtn.addEventListener("click", () => {
    const keyword = filterInput.value.trim().toLowerCase();

    if (!keyword) {
        updateResultTable(allPredictions);
        return;
    }

    const filteredPredictions = allPredictions.filter(p =>
        (p.name || "").toLowerCase() === keyword
    );

    updateResultTable(filteredPredictions);
});

resetFilterBtn.addEventListener("click", () => {
    filterInput.value = "";
    updateResultTable(allPredictions);
});

// 视频检测
videoUploadForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const file = videoFileInput.files[0];
    if (!file) {
        alert("请选择视频文件");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("confidence", document.getElementById("videoConfidence").value);

    videoSubmitBtn.disabled = true;
    videoLoader.style.display = "block";
    videoResultCard.style.display = "none";

    try {
        const response = await fetch("/api/detect/video", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `服务器返回状态码 ${response.status}`);
        }

        const result = await response.json();
        console.log("视频检测结果:", result);

        if (result.video_url) {
            resultVideo.src = result.video_url;
            videoInfo.innerHTML = `总帧数：${result.frame_count || "-"} 帧`;
            videoResultCard.style.display = "block";
        } else {
            alert("视频检测完成，但没有返回视频地址");
        }

    } catch (error) {
        console.error("视频检测失败:", error);
        alert("视频检测失败：" + error.message);
    } finally {
        videoSubmitBtn.disabled = false;
        videoLoader.style.display = "none";
    }
});