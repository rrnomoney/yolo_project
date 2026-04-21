document.addEventListener("DOMContentLoaded", function () {
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

                <!-- 放大检测图 -->
                <div class="detail-image-box">
                    <img src="${data.image || ''}" class="detail-image" alt="检测结果图">
                </div>

               <div class="download-box">
                    <a href="${data.image}" download class="download-btn">下载检测图</a>
                </div>

                <!-- 表格 -->
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
});