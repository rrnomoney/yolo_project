document.addEventListener("DOMContentLoaded", function () {
    initImageHistory();
    initVideoHistory();
    initHistoryFilters();
});

// 处理图片历史记录的摘要、缩略图、详情表格
function initImageHistory() {
    document.querySelectorAll(".result-container").forEach(container => {
        try {
            const jsonText = container.getAttribute("data-json");
            if (!jsonText) return;

            const data = JSON.parse(jsonText);

            // 显示缩略图
            const img = container.parentElement.parentElement.querySelector(".thumb");
            if (img && data.image) {
                img.src = data.image;
            }

            // 统计类别数量
            const predictions = data.predictions || [];
            const countMap = {};

            predictions.forEach(p => {
                const name = p.name || "unknown";
                countMap[name] = (countMap[name] || 0) + 1;
            });

            let summaryHtml = `<div class="result-summary">`;
            summaryHtml += `<span class="summary-tag summary-total">总目标：${predictions.length} 个</span>`;

            for (const key in countMap) {
                summaryHtml += `<span class="summary-tag">${key}：${countMap[key]} 个</span>`;
            }

            summaryHtml += `</div>`;

            let tableRows = "";

            predictions.forEach(p => {
                const confPercent = ((p.confidence || 0) * 100).toFixed(1) + "%";
                tableRows += `
                    <tr>
                        <td>${p.name || "unknown"}</td>
                        <td>${confPercent}</td>
                        <td>${Math.round(p.xmin || 0)}, ${Math.round(p.ymin || 0)}, ${Math.round(p.xmax || 0)}, ${Math.round(p.ymax || 0)}</td>
                    </tr>
                `;
            });

            let detailHtml = `
                <div class="details-box">
                    <details>
                        <summary>查看详情</summary>

                        <div class="detail-content">
                            <div class="detail-image-box">
                                <img src="${data.image || ''}" class="detail-image" alt="检测结果图">
                            </div>

                            <div class="download-box">
                                <a href="${data.image}" download class="download-btn">下载检测图</a>
                            </div>

                            <div class="detail-table-wrapper">
                                <table class="detail-table">
                                    <thead>
                                        <tr>
                                            <th>目标名称</th>
                                            <th>置信度</th>
                                            <th>坐标</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${tableRows}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </details>
                </div>
            `;

            container.innerHTML = summaryHtml + detailHtml;

        } catch (e) {
            container.innerHTML = `<span style="color:#d9534f;">解析失败</span>`;
        }
    });
}

// 处理视频历史记录的累计统计 + 去重统计
function initVideoHistory() {
    document.querySelectorAll(".video-result-container").forEach(container => {
        try {
            const jsonText = container.getAttribute("data-json");
            if (!jsonText) return;

            const data = JSON.parse(jsonText);
            const classCountMap = data.class_count_map || {};
            const uniqueCountMap = data.unique_count_map || {};
            const summaryBox = container.querySelector(".video-class-summary");

            if (!summaryBox) return;

            let html = "";

            // 第一行：累计统计
            if (Object.keys(classCountMap).length > 0) {
                html += `<div class="result-summary">`;
                html += `<span class="summary-tag summary-total">累计统计：</span>`;

                for (const key in classCountMap) {
                    html += `<span class="summary-tag">${key}：${classCountMap[key]} 次</span>`;
                }

                html += `</div>`;
            }

            // 第二行：去重统计
            if (Object.keys(uniqueCountMap).length > 0) {
                html += `<div class="result-summary">`;
                html += `<span class="summary-tag summary-total">去重统计：</span>`;

                for (const key in uniqueCountMap) {
                    html += `<span class="summary-tag">${key}：${uniqueCountMap[key]} 个</span>`;
                }

                html += `</div>`;
            }

            if (!html) {
                html = `<span class="summary-tag">未统计到类别信息</span>`;
            }

            summaryBox.innerHTML = html;

        } catch (e) {
            console.error("视频记录解析失败:", e);
        }
    });
}

// 初始化搜索、筛选、排序
function initHistoryFilters() {
    const searchInput = document.getElementById("searchInput");
    const typeFilter = document.getElementById("typeFilter");
    const sortOrder = document.getElementById("sortOrder");
    const applyFilterBtn = document.getElementById("applyFilterBtn");
    const resetFilterBtn = document.getElementById("resetFilterBtn");
    const tableBody = document.getElementById("historyTableBody");

    if (!tableBody) return;

    const allRows = Array.from(tableBody.querySelectorAll(".history-row"));

    function applyFilters() {
        const keyword = searchInput.value.trim().toLowerCase();
        const selectedType = typeFilter.value;
        const selectedSort = sortOrder.value;

        let filteredRows = allRows.filter(row => {
            const fileName = (row.getAttribute("data-file-name") || "").toLowerCase();
            const fileType = row.getAttribute("data-file-type") || "image";
            const rowText = (row.innerText || "").toLowerCase();

            const matchKeyword = !keyword || fileName.includes(keyword) || rowText.includes(keyword);
            const matchType = selectedType === "all" || fileType === selectedType;

            return matchKeyword && matchType;
        });

        filteredRows.sort((a, b) => {
            const timeA = new Date((a.getAttribute("data-detect-time") || "").replace(/-/g, "/")).getTime();
            const timeB = new Date((b.getAttribute("data-detect-time") || "").replace(/-/g, "/")).getTime();

            return selectedSort === "desc" ? timeB - timeA : timeA - timeB;
        });

        allRows.forEach(row => row.remove());
        filteredRows.forEach(row => tableBody.appendChild(row));
    }

    function resetFilters() {
        searchInput.value = "";
        typeFilter.value = "all";
        sortOrder.value = "desc";

        allRows.forEach(row => row.remove());
        allRows
            .sort((a, b) => {
                const timeA = new Date((a.getAttribute("data-detect-time") || "").replace(/-/g, "/")).getTime();
                const timeB = new Date((b.getAttribute("data-detect-time") || "").replace(/-/g, "/")).getTime();
                return timeB - timeA;
            })
            .forEach(row => tableBody.appendChild(row));
    }

    applyFilterBtn.addEventListener("click", applyFilters);
    resetFilterBtn.addEventListener("click", resetFilters);
}