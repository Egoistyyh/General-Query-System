const state = {
    columns: [],
    recognition: null
};

const tableNameEl = document.querySelector("#tableName");
const conditionListEl = document.querySelector("#conditionList");
const templateEl = document.querySelector("#conditionTemplate");
const wherePreviewEl = document.querySelector("#wherePreview");
const sqlPreviewEl = document.querySelector("#sqlPreview");
const resultTableEl = document.querySelector("#resultTable");
const messageBoxEl = document.querySelector("#messageBox");
const voiceTextEl = document.querySelector("#voiceText");
const voiceHintEl = document.querySelector("#voiceHint");

document.querySelector("#reloadColumnsBtn").addEventListener("click", loadColumns);
document.querySelector("#addConditionBtn").addEventListener("click", () => addConditionRow());
document.querySelector("#clearBtn").addEventListener("click", clearConditions);
document.querySelector("#runQueryBtn").addEventListener("click", runQuery);
document.querySelector("#parseVoiceBtn").addEventListener("click", parseVoiceText);
document.querySelector("#startVoiceBtn").addEventListener("click", startVoiceRecognition);
tableNameEl.addEventListener("change", loadColumns);

loadColumns();

async function loadColumns() {
    const tableName = tableNameEl.value;
    const response = await fetch(`/api/tables/${encodeURIComponent(tableName)}/columns`);
    state.columns = await response.json();
    conditionListEl.innerHTML = "";
    addConditionRow();
    await runQuery();
}

function addConditionRow(condition = {}) {
    const node = templateEl.content.firstElementChild.cloneNode(true);
    const columnSelect = node.querySelector(".column-select");
    const operatorSelect = node.querySelector(".operator-select");
    const valueInput = node.querySelector(".value-input");
    const connectorSelect = node.querySelector(".connector-select");

    state.columns.forEach((column) => {
        const option = document.createElement("option");
        option.value = column.name;
        option.textContent = `${column.label} (${column.dataType})`;
        columnSelect.appendChild(option);
    });

    columnSelect.value = condition.column || state.columns[0]?.name || "";
    valueInput.value = condition.value || "";
    connectorSelect.value = condition.connector || "AND";

    columnSelect.addEventListener("change", () => {
        fillOperators(operatorSelect, columnSelect.value);
        refreshWherePreview();
    });
    operatorSelect.addEventListener("change", refreshWherePreview);
    valueInput.addEventListener("input", refreshWherePreview);
    connectorSelect.addEventListener("change", refreshWherePreview);
    node.querySelector(".remove-btn").addEventListener("click", () => {
        node.remove();
        refreshWherePreview();
    });

    fillOperators(operatorSelect, columnSelect.value, condition.operator);
    conditionListEl.appendChild(node);
    refreshWherePreview();
}

function fillOperators(operatorSelect, columnName, preferredOperator) {
    const column = state.columns.find((item) => item.name === columnName);
    operatorSelect.innerHTML = "";
    (column?.operators || ["="]).forEach((operator) => {
        const option = document.createElement("option");
        option.value = operator;
        option.textContent = operator;
        operatorSelect.appendChild(option);
    });
    operatorSelect.value = preferredOperator || operatorSelect.options[0]?.value || "=";
}

function collectConditions() {
    return [...conditionListEl.querySelectorAll(".condition-row")]
        .map((row, index) => ({
            column: row.querySelector(".column-select").value,
            operator: row.querySelector(".operator-select").value,
            value: row.querySelector(".value-input").value,
            connector: index === 0 ? "AND" : row.querySelector(".connector-select").value
        }))
        .filter((condition) => condition.column && condition.operator)
        .filter((condition) => condition.operator === "Is" || condition.operator === "Is Not" || condition.value.trim() !== "");
}

async function refreshWherePreview() {
    const conditions = collectConditions();
    if (conditions.length === 0) {
        wherePreviewEl.value = "无查询条件";
        return;
    }
    const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ tableName: tableNameEl.value, conditions })
    });
    if (response.ok) {
        const data = await response.json();
        wherePreviewEl.value = data.whereClause;
    }
}

async function parseVoiceText() {
    clearMessage();
    const text = voiceTextEl.value.trim();
    if (!text) {
        showMessage("请先语音输入或手动输入查询指令。");
        return;
    }
    const response = await fetch("/api/parse-voice", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ tableName: tableNameEl.value, text })
    });
    const data = await response.json();
    if (!response.ok) {
        showMessage(data.message || "语音文本解析失败");
        return;
    }
    conditionListEl.innerHTML = "";
    if (data.conditions.length === 0) {
        addConditionRow();
        showMessage("没有识别到有效条件，请换一种表达方式，例如：性别为男，年龄小于18。");
        return;
    }
    data.conditions.forEach((condition) => addConditionRow(condition));
    wherePreviewEl.value = data.wherePreview;
    await runQuery();
}

async function runQuery() {
    clearMessage();
    const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ tableName: tableNameEl.value, conditions: collectConditions() })
    });
    const data = await response.json();
    if (!response.ok) {
        showMessage(data.message || "查询失败");
        return;
    }
    sqlPreviewEl.textContent = data.sql;
    wherePreviewEl.value = data.whereClause;
    renderTable(data.headers, data.rows);
}

function renderTable(headers, rows) {
    const thead = resultTableEl.querySelector("thead");
    const tbody = resultTableEl.querySelector("tbody");
    thead.innerHTML = "";
    tbody.innerHTML = "";

    const headRow = document.createElement("tr");
    headers.forEach((header) => {
        const th = document.createElement("th");
        const column = state.columns.find((item) => item.name === header);
        th.textContent = column ? column.label : header;
        headRow.appendChild(th);
    });
    thead.appendChild(headRow);

    if (rows.length === 0) {
        const emptyRow = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = headers.length;
        cell.textContent = "没有符合条件的数据";
        emptyRow.appendChild(cell);
        tbody.appendChild(emptyRow);
        return;
    }

    rows.forEach((row) => {
        const tr = document.createElement("tr");
        headers.forEach((header) => {
            const td = document.createElement("td");
            td.textContent = row[header] ?? "";
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
}

function clearConditions() {
    conditionListEl.innerHTML = "";
    addConditionRow();
    wherePreviewEl.value = "无查询条件";
}

function startVoiceRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        showMessage("当前浏览器不支持 Web Speech API，请使用 Chrome/Edge，或直接手动输入文本。");
        return;
    }

    state.recognition = new SpeechRecognition();
    state.recognition.lang = "zh-CN";
    state.recognition.interimResults = false;
    state.recognition.maxAlternatives = 1;
    voiceHintEl.textContent = "正在聆听，请说出查询指令。";

    state.recognition.onresult = (event) => {
        voiceTextEl.value = event.results[0][0].transcript;
        voiceHintEl.textContent = "语音识别完成，可以点击“分析文本生成条件”。";
    };
    state.recognition.onerror = (event) => {
        showMessage(`语音识别失败：${event.error}`);
        voiceHintEl.textContent = "可以改用手动输入文本。";
    };
    state.recognition.onend = () => {
        if (!voiceTextEl.value.trim()) {
            voiceHintEl.textContent = "没有识别到内容，请重试或手动输入。";
        }
    };
    state.recognition.start();
}

function showMessage(message) {
    messageBoxEl.hidden = false;
    messageBoxEl.textContent = message;
}

function clearMessage() {
    messageBoxEl.hidden = true;
    messageBoxEl.textContent = "";
}
