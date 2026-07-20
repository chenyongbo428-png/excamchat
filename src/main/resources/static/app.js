(function () {
    const TOKEN_KEY = "cheat.token";
    const ANSWER_MODE_KEY = "cheat.answerMode";
    const state = {
        token: localStorage.getItem(TOKEN_KEY) || "",
        currentUser: null,
        models: [],
        sessions: [],
        currentSessionId: null,
        currentImageId: null,
        currentImageUrl: "",
        currentImageObjectUrl: "",
        currentImageNaturalWidth: 0,
        currentImageNaturalHeight: 0,
        sendingMessage: false,
        answerMode: localStorage.getItem(ANSWER_MODE_KEY) === "direct" ? "direct" : "guided",
        messages: [],
        aiAnnotations: [],
        canvasTool: "pen",
        canvasColor: "#ff6b6b",
        canvasSize: 3,
        canvasVersion: null,
        canvasObjects: [],
        canvasHistory: [],
        drawing: null,
        replay: {
            loaded: false,
            active: false,
            playing: false,
            timer: null,
            stepIndex: 0,
            timeline: []
        }
    };

    const dom = {
        logoutBtn: document.getElementById("logoutBtn"),
        authStatus: document.getElementById("authStatus"),
        modelSelect: document.getElementById("modelSelect"),
        uploadForm: document.getElementById("uploadForm"),
        imageFileInput: document.getElementById("imageFileInput"),
        imageMeta: document.getElementById("imageMeta"),
        createSessionForm: document.getElementById("createSessionForm"),
        sessionTitleInput: document.getElementById("sessionTitleInput"),
        subjectCodeInput: document.getElementById("subjectCodeInput"),
        gradeLevelInput: document.getElementById("gradeLevelInput"),
        refreshSessionsBtn: document.getElementById("refreshSessionsBtn"),
        sessionList: document.getElementById("sessionList"),
        sessionHeading: document.getElementById("sessionHeading"),
        sessionMeta: document.getElementById("sessionMeta"),
        messageList: document.getElementById("messageList"),
        messageForm: document.getElementById("messageForm"),
        messageInput: document.getElementById("messageInput"),
        answerModeSelect: document.getElementById("answerModeSelect"),
        messageHint: document.getElementById("messageHint"),
        loadReplayBtn: document.getElementById("loadReplayBtn"),
        replayPlayBtn: document.getElementById("replayPlayBtn"),
        replayPrevBtn: document.getElementById("replayPrevBtn"),
        replayNextBtn: document.getElementById("replayNextBtn"),
        replayResetBtn: document.getElementById("replayResetBtn"),
        replayExitBtn: document.getElementById("replayExitBtn"),
        replaySpeedSelect: document.getElementById("replaySpeedSelect"),
        replayStatus: document.getElementById("replayStatus"),
        replayTimeline: document.getElementById("replayTimeline"),
        questionImage: document.getElementById("questionImage"),
        annotationCanvas: document.getElementById("annotationCanvas"),
        colorInput: document.getElementById("colorInput"),
        sizeInput: document.getElementById("sizeInput"),
        canvasExport: document.getElementById("canvasExport"),
        clearCanvasBtn: document.getElementById("clearCanvasBtn"),
        undoCanvasBtn: document.getElementById("undoCanvasBtn"),
        exportCanvasBtn: document.getElementById("exportCanvasBtn"),
        toast: document.getElementById("toast"),
        toolButtons: Array.from(document.querySelectorAll("[data-tool]"))
    };

    const canvasContext = dom.annotationCanvas.getContext("2d");

    function showToast(message, isError) {
        dom.toast.textContent = message;
        dom.toast.classList.remove("hidden");
        dom.toast.style.background = isError ? "rgba(161, 42, 42, 0.94)" : "rgba(31, 28, 22, 0.92)";
        clearTimeout(showToast.timer);
        showToast.timer = setTimeout(function () {
            dom.toast.classList.add("hidden");
        }, 2600);
    }

    function nextObjectId(prefix) {
        return (prefix || "obj") + "-" + Date.now().toString(36) + "-" + Math.random().toString(16).slice(2, 8);
    }

    async function api(path, options) {
        const requestOptions = options || {};
        const headers = new Headers(requestOptions.headers || {});
        if (!(requestOptions.body instanceof FormData) && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json; charset=utf-8");
        }
        if (state.token) {
            headers.set("Authorization", "Bearer " + state.token);
        }

        const response = await fetch(path, {
            ...requestOptions,
            headers: headers
        });

        if (!response.ok) {
            let message = response.statusText;
            try {
                const payload = await response.json();
                message = payload.message || payload.error || message;
            } catch (error) {
                const text = await response.text();
                if (text) {
                    message = text;
                }
            }
            throw new Error(message);
        }

        const contentType = response.headers.get("Content-Type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }
        return response;
    }

    function escapeHtml(value) {
        return String(value || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;");
    }

    function formatInlineMarkdown(value) {
        return escapeHtml(value)
            .replace(/\\\((.*?)\\\)/g, '<span class="math-inline">$1</span>')
            .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
    }

    function renderRichText(value) {
        const lines = String(value || "").split(/\r?\n/);
        const html = [];
        let listOpen = false;

        function closeList() {
            if (listOpen) {
                html.push("</ol>");
                listOpen = false;
            }
        }

        lines.forEach(function (line) {
            const trimmed = line.trim();
            if (!trimmed) {
                closeList();
                html.push("<br>");
                return;
            }
            const heading = /^(#{1,4})\s+(.+)$/.exec(trimmed);
            if (heading) {
                closeList();
                const level = Math.min(4, heading[1].length + 1);
                html.push("<h" + level + ">" + formatInlineMarkdown(heading[2]) + "</h" + level + ">");
                return;
            }
            const numbered = /^(\d+)\.\s+(.+)$/.exec(trimmed);
            if (numbered) {
                if (!listOpen) {
                    html.push('<ol class="answer-steps">');
                    listOpen = true;
                }
                html.push("<li>" + formatInlineMarkdown(numbered[2]) + "</li>");
                return;
            }
            const bullet = /^[-*]\s+(.+)$/.exec(trimmed);
            if (bullet) {
                if (!listOpen) {
                    html.push('<ol class="answer-steps compact">');
                    listOpen = true;
                }
                html.push("<li>" + formatInlineMarkdown(bullet[1]) + "</li>");
                return;
            }
            closeList();
            html.push("<p>" + formatInlineMarkdown(trimmed) + "</p>");
        });
        closeList();
        return html.join("");
    }

    function formatDateTime(value) {
        if (!value) {
            return "";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        return date.toLocaleString("zh-CN", {
            hour12: false,
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function saveToken(token) {
        state.token = token || "";
        if (state.token) {
            localStorage.setItem(TOKEN_KEY, state.token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
        renderAuthStatus();
    }

    function navigateToAuthPage() {
        window.location.replace("/index.html");
    }

    function renderAuthStatus() {
        if (!dom.authStatus) {
            return;
        }
        if (state.currentUser) {
            const nickname = state.currentUser.nickname || state.currentUser.username;
            dom.authStatus.textContent = "已登录：" + nickname + "（" + state.currentUser.username + "）";
            return;
        }
        if (state.token) {
            dom.authStatus.textContent = "检测到本地登录态，正在恢复...";
            return;
        }
        dom.authStatus.textContent = "未登录";
    }

    function renderModelSelect() {
        dom.modelSelect.innerHTML = "";
        if (!state.models.length) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = "暂无可用模型";
            dom.modelSelect.appendChild(option);
            return;
        }
        state.models.forEach(function (model) {
            const option = document.createElement("option");
            option.value = model.modelCode;
            option.textContent = model.displayName + " / " + model.providerCode;
            dom.modelSelect.appendChild(option);
        });
    }

    async function loadModels() {
        try {
            const result = await api("/api/models/enabled", { method: "GET" });
            state.models = result.data || [];
            renderModelSelect();
        } catch (error) {
            state.models = [];
            renderModelSelect();
            showToast("模型列表加载失败：" + error.message, true);
        }
    }

    function renderSessionList() {
        dom.sessionList.innerHTML = "";
        if (!state.sessions.length) {
            const empty = document.createElement("li");
            empty.className = "subtle";
            empty.textContent = "还没有会话，先上传题图并创建一个。";
            dom.sessionList.appendChild(empty);
            return;
        }

        state.sessions.forEach(function (session) {
            const item = document.createElement("li");
            item.className = "session-item" + (session.sessionId === state.currentSessionId ? " active" : "");
            item.innerHTML = [
                '<div class="session-title">' + escapeHtml(session.title || "未命名会话") + "</div>",
                '<div class="subtle">' + escapeHtml(session.modelCode || "-") + " · " +
                    escapeHtml(formatDateTime(session.lastMessageAt || session.createdAt)) + "</div>"
            ].join("");
            item.addEventListener("click", function () {
                selectSession(session.sessionId);
            });
            dom.sessionList.appendChild(item);
        });
    }

    async function loadSessions() {
        if (!state.token) {
            state.sessions = [];
            renderSessionList();
            return;
        }
        try {
            const result = await api("/api/sessions?page=1&pageSize=20", { method: "GET" });
            state.sessions = result.data ? result.data.items || [] : [];
            renderSessionList();
        } catch (error) {
            showToast("会话列表加载失败：" + error.message, true);
        }
    }

    function extractAnnotations(messages) {
        return (messages || [])
            .filter(function (message) {
                return Array.isArray(message.annotationSummary) && message.annotationSummary.length > 0;
            })
            .flatMap(function (message) {
                return message.annotationSummary.map(function (annotation, index) {
                    return {
                        ...annotation,
                        objectId: annotation.id || ("ai-" + message.messageId + "-" + index),
                        source: "AI",
                        messageId: message.messageId,
                        hintLevel: message.hintLevel,
                        guidanceStage: message.guidanceStage,
                        teacherIntent: message.teacherIntent
                    };
                });
            });
    }

    function renderMessages() {
        dom.messageList.innerHTML = "";
        if (!state.messages.length) {
            const empty = document.createElement("div");
            empty.className = "subtle";
            empty.textContent = state.replay.active ? "回放尚未走到聊天消息。" : "当前还没有消息。";
            dom.messageList.appendChild(empty);
            return;
        }

        state.messages.forEach(function (message) {
            const card = document.createElement("article");
            card.className = "message-card " + String(message.roleCode || "assistant").toLowerCase() +
                (message.streaming ? " streaming" : "");
            const tags = (message.annotationSummary || []).map(function (annotation) {
                const label = annotation.label || annotation.type || "annotation";
                return '<span class="annotation-tag">' + escapeHtml(label) + "</span>";
            }).join("");
            const bodyHtml = String(message.roleCode || "").toUpperCase() === "ASSISTANT"
                ? renderRichText(message.contentText || "")
                : '<p>' + escapeHtml(message.contentText || "") + "</p>";
            card.innerHTML = [
                '<div class="message-role">' + escapeHtml(message.roleCode || "") + "</div>",
                '<div class="message-body">' + bodyHtml + "</div>",
                '<div class="subtle">' +
                    escapeHtml(message.guidanceStage || "observe") +
                    (message.hintLevel ? " · Hint " + escapeHtml(message.hintLevel) : "") +
                    (message.teacherIntent ? " · " + escapeHtml(message.teacherIntent) : "") +
                    (message.createdAt ? " · " + escapeHtml(formatDateTime(message.createdAt)) : "") +
                "</div>",
                tags ? '<div class="annotation-tags">' + tags + "</div>" : ""
            ].join("");
            dom.messageList.appendChild(card);
        });
        dom.messageList.scrollTop = dom.messageList.scrollHeight;
    }

    function stopReplayTimer() {
        if (state.replay.timer) {
            clearTimeout(state.replay.timer);
            state.replay.timer = null;
        }
        state.replay.playing = false;
        dom.replayPlayBtn.textContent = "播放";
    }

    function resetReplayState(clearTimeline) {
        stopReplayTimer();
        state.replay.active = false;
        state.replay.stepIndex = 0;
        if (clearTimeline) {
            state.replay.loaded = false;
            state.replay.timeline = [];
        }
        renderReplayTimeline();
    }

    function replayStepTitle(item) {
        if (!item) {
            return "等待回放";
        }
        if (item.stepType === "MESSAGE") {
            return (item.roleCode === "USER" ? "学生发言" : "老师引导") + " #" + item.stepNo;
        }
        if (item.stepType === "AI_ANNOTATION") {
            return "AI 标注 #" + item.stepNo;
        }
        if (item.stepType === "CANVAS_OPERATION") {
            return "画布操作 #" + item.stepNo;
        }
        return item.stepType + " #" + item.stepNo;
    }

    function replayStepMeta(item) {
        if (!item) {
            return "选择会话后可载入回放时间线。";
        }
        if (item.stepType === "MESSAGE") {
            return (item.contentText || "").slice(0, 48) || "空消息";
        }
        if (item.stepType === "AI_ANNOTATION") {
            return item.annotation?.label || item.annotation?.type || "AI 标注";
        }
        if (item.stepType === "CANVAS_OPERATION") {
            return [
                item.operation?.operationType,
                item.operation?.layerType,
                "seq=" + (item.operation?.sequenceNo || "-")
            ].filter(Boolean).join(" · ");
        }
        return formatDateTime(item.createdAt);
    }

    function renderReplayTimeline() {
        const total = state.replay.timeline.length;
        const current = total ? state.replay.timeline[Math.max(0, state.replay.stepIndex - 1)] : null;
        if (!state.currentSessionId) {
            dom.replayStatus.textContent = "选择会话后可载入回放时间线。";
        } else if (!state.replay.loaded) {
            dom.replayStatus.textContent = "当前会话尚未载入回放。";
        } else if (!total) {
            dom.replayStatus.textContent = "当前会话暂无可回放事件。";
        } else {
            dom.replayStatus.textContent = [
                state.replay.active ? "回放中" : "已载入",
                state.replay.stepIndex + " / " + total,
                current ? replayStepTitle(current) : "起点"
            ].join(" · ");
        }

        dom.replayPlayBtn.textContent = state.replay.playing ? "暂停" : "播放";
        dom.replayTimeline.innerHTML = "";
        if (!total) {
            const empty = document.createElement("li");
            empty.className = "subtle";
            empty.textContent = state.replay.loaded ? "暂无事件，发送消息或绘制标注后再试。" : "点击“载入回放”获取当前会话时间线。";
            dom.replayTimeline.appendChild(empty);
            return;
        }

        state.replay.timeline.forEach(function (item, index) {
            const row = document.createElement("li");
            row.className = "replay-step" +
                (index < state.replay.stepIndex ? " applied" : "") +
                (index === state.replay.stepIndex - 1 ? " current" : "");
            row.innerHTML = [
                '<button type="button" class="replay-step-button">',
                '<span class="replay-step-no">' + escapeHtml(item.stepNo) + "</span>",
                '<span><strong>' + escapeHtml(replayStepTitle(item)) + "</strong>",
                '<small>' + escapeHtml(replayStepMeta(item)) + "</small></span>",
                "</button>"
            ].join("");
            row.querySelector("button").addEventListener("click", function () {
                stopReplayTimer();
                applyReplayToStep(index + 1);
            });
            dom.replayTimeline.appendChild(row);
        });
    }

    function buildCanvasExportPayload() {
        return buildCanvasSnapshot();
    }

    function updateCanvasExport() {
        dom.canvasExport.textContent = JSON.stringify(buildCanvasExportPayload(), null, 2);
    }

    function pushCanvasHistory() {
        state.canvasHistory.push(JSON.stringify(state.canvasObjects));
        if (state.canvasHistory.length > 40) {
            state.canvasHistory.shift();
        }
    }

    function resetCanvasState() {
        state.canvasObjects = [];
        state.canvasHistory = [JSON.stringify(state.canvasObjects)];
        state.drawing = null;
    }

    function toImageX(canvasX) {
        if (!dom.annotationCanvas.width || !state.currentImageNaturalWidth) {
            return canvasX || 0;
        }
        return (Number(canvasX || 0) / dom.annotationCanvas.width) * state.currentImageNaturalWidth;
    }

    function toImageY(canvasY) {
        if (!dom.annotationCanvas.height || !state.currentImageNaturalHeight) {
            return canvasY || 0;
        }
        return (Number(canvasY || 0) / dom.annotationCanvas.height) * state.currentImageNaturalHeight;
    }

    function toImageWidth(canvasWidth) {
        if (!dom.annotationCanvas.width || !state.currentImageNaturalWidth) {
            return canvasWidth || 0;
        }
        return (Number(canvasWidth || 0) / dom.annotationCanvas.width) * state.currentImageNaturalWidth;
    }

    function toImageHeight(canvasHeight) {
        if (!dom.annotationCanvas.height || !state.currentImageNaturalHeight) {
            return canvasHeight || 0;
        }
        return (Number(canvasHeight || 0) / dom.annotationCanvas.height) * state.currentImageNaturalHeight;
    }

    function buildCanvasSnapshot() {
        return {
            schemaVersion: "1.0",
            background: {
                imageId: String(state.currentImageId || ""),
                width: state.currentImageNaturalWidth || 0,
                height: state.currentImageNaturalHeight || 0
            },
            viewport: {
                zoom: 1,
                panX: 0,
                panY: 0
            },
            layers: [
                {
                    layerId: "ai-layer",
                    layerType: "AI",
                    visible: true,
                    locked: true,
                    objects: state.aiAnnotations.map(toProtocolObject)
                },
                {
                    layerId: "user-layer",
                    layerType: "USER",
                    visible: true,
                    locked: false,
                    objects: state.canvasObjects.map(toProtocolObject)
                }
            ]
        };
    }

    function toProtocolObject(object) {
        const protocolObject = {
            objectId: object.objectId || object.id || nextObjectId("obj"),
            type: object.type,
            source: object.source || "USER",
            style: {
                strokeColor: object.color || object.style?.strokeColor || state.canvasColor,
                fillColor: object.fillColor || object.style?.fillColor || "transparent",
                strokeWidth: object.size || object.style?.strokeWidth || state.canvasSize,
                opacity: object.opacity || object.style?.opacity || 1
            }
        };

        if (object.type === "pen" || object.type === "path") {
            protocolObject.type = "path";
            protocolObject.points = (object.points || []).map(function (point) {
                return [toImageX(point.x), toImageY(point.y)];
            });
        } else if (object.type === "rect" || object.type === "highlight") {
            protocolObject.x = toImageX(object.x);
            protocolObject.y = toImageY(object.y);
            protocolObject.width = toImageWidth(object.width);
            protocolObject.height = toImageHeight(object.height);
            if (object.label) {
                protocolObject.label = object.label;
            }
        } else if (object.type === "arrow") {
            protocolObject.x = toImageX(object.x);
            protocolObject.y = toImageY(object.y);
            protocolObject.toX = toImageX(object.toX);
            protocolObject.toY = toImageY(object.toY);
            if (object.label) {
                protocolObject.label = object.label;
            }
        } else if (object.type === "text") {
            protocolObject.x = toImageX(object.x);
            protocolObject.y = toImageY(object.y);
            protocolObject.text = object.text || object.label || "";
        }

        if (object.messageId || object.teacherIntent) {
            protocolObject.meta = {
                messageId: object.messageId,
                teacherIntent: object.teacherIntent
            };
        }
        return protocolObject;
    }

    function fromProtocolObject(object) {
        const type = object.type === "path" ? "pen" : object.type;
        const style = object.style || {};
        const converted = {
            objectId: object.objectId,
            type: type,
            source: object.source || "USER",
            color: style.strokeColor || style.fillColor || "#ff6b6b",
            size: style.strokeWidth || 3,
            opacity: style.opacity || 1,
            label: object.label,
            messageId: object.meta ? object.meta.messageId : undefined,
            teacherIntent: object.meta ? object.meta.teacherIntent : undefined
        };

        if (type === "pen") {
            converted.points = (object.points || []).map(function (point) {
                return {
                    x: toCanvasX(point[0]),
                    y: toCanvasY(point[1])
                };
            });
        } else if (type === "rect" || type === "highlight") {
            converted.x = toCanvasX(object.x);
            converted.y = toCanvasY(object.y);
            converted.width = toCanvasWidth(object.width);
            converted.height = toCanvasHeight(object.height);
        } else if (type === "arrow") {
            converted.x = toCanvasX(object.x);
            converted.y = toCanvasY(object.y);
            converted.toX = toCanvasX(object.toX);
            converted.toY = toCanvasY(object.toY);
        } else if (type === "text") {
            converted.x = toCanvasX(object.x);
            converted.y = toCanvasY(object.y);
            converted.text = object.text || object.label || "";
        }
        return converted;
    }

    async function loadCanvasDocument() {
        if (!state.currentSessionId) {
            resetCanvasState();
            redrawCanvas();
            updateCanvasExport();
            return;
        }
        const result = await api("/api/canvas/" + state.currentSessionId, { method: "GET" });
        const document = result.data;
        state.canvasVersion = document.version;
        const layers = document.snapshot && Array.isArray(document.snapshot.layers) ? document.snapshot.layers : [];
        const aiLayer = layers.find(function (layer) {
            return layer.layerType === "AI";
        });
        const userLayer = layers.find(function (layer) {
            return layer.layerType === "USER";
        });
        if (aiLayer && Array.isArray(aiLayer.objects) && aiLayer.objects.length) {
            state.aiAnnotations = aiLayer.objects.map(fromProtocolObject);
        }
        state.canvasObjects = userLayer && Array.isArray(userLayer.objects)
            ? userLayer.objects.map(fromProtocolObject)
            : [];
        state.canvasHistory = [JSON.stringify(state.canvasObjects)];
        redrawCanvas();
        updateCanvasExport();
    }

    async function saveCanvasSnapshot() {
        if (!state.currentSessionId) {
            updateCanvasExport();
            return;
        }
        try {
            const result = await api("/api/canvas/" + state.currentSessionId, {
                method: "PUT",
                body: JSON.stringify({
                    version: state.canvasVersion,
                    snapshot: buildCanvasSnapshot()
                })
            });
            state.canvasVersion = result.data.version;
            updateCanvasExport();
        } catch (error) {
            showToast("画布保存失败：" + error.message, true);
            updateCanvasExport();
        }
    }

    async function appendCanvasOperation(operationType, layerType, payload) {
        if (!state.currentSessionId) {
            return;
        }
        try {
            const result = await api("/api/canvas/" + state.currentSessionId + "/operations", {
                method: "POST",
                body: JSON.stringify({
                    operations: [
                        {
                            operationType: operationType,
                            layerType: layerType,
                            payload: payload
                        }
                    ]
                })
            });
            state.canvasVersion = result.data.version;
        } catch (error) {
            showToast("画布操作记录失败：" + error.message, true);
        }
    }

    async function loadReplay() {
        if (!state.currentSessionId) {
            showToast("请先选择一个会话。", true);
            return;
        }
        stopReplayTimer();
        try {
            const result = await api("/api/replay/" + state.currentSessionId, { method: "GET" });
            state.replay.loaded = true;
            state.replay.active = true;
            state.replay.stepIndex = 0;
            state.replay.timeline = result.data ? result.data.timeline || [] : [];
            applyReplayToStep(0);
            showToast("回放时间线已载入");
        } catch (error) {
            showToast("回放载入失败：" + error.message, true);
        }
    }

    function applyReplayToStep(stepIndex) {
        if (!state.replay.loaded) {
            return;
        }
        const boundedIndex = Math.max(0, Math.min(stepIndex, state.replay.timeline.length));
        state.replay.active = true;
        state.replay.stepIndex = boundedIndex;
        state.messages = [];
        state.aiAnnotations = [];
        state.canvasObjects = [];
        state.canvasHistory = [JSON.stringify(state.canvasObjects)];
        state.drawing = null;

        state.replay.timeline.slice(0, boundedIndex).forEach(applyReplayItem);
        renderMessages();
        redrawCanvas();
        updateCanvasExport();
        renderReplayTimeline();
    }

    function applyReplayItem(item) {
        if (item.stepType === "MESSAGE") {
            state.messages.push({
                messageId: item.messageId,
                roleCode: item.roleCode,
                contentText: item.contentText,
                createdAt: item.createdAt,
                annotationSummary: []
            });
            return;
        }

        if (item.stepType === "AI_ANNOTATION" && item.annotation) {
            const annotation = {
                ...item.annotation,
                objectId: item.annotation.objectId || ("replay-ai-" + item.stepNo),
                source: "AI",
                messageId: item.messageId
            };
            state.aiAnnotations.push(annotation);
            const message = state.messages.find(function (candidate) {
                return candidate.messageId === item.messageId;
            });
            if (message) {
                message.annotationSummary = message.annotationSummary || [];
                message.annotationSummary.push(annotation);
            }
            return;
        }

        if (item.stepType === "CANVAS_OPERATION" && item.operation) {
            applyReplayCanvasOperation(item.operation);
        }
    }

    function applyReplayCanvasOperation(operation) {
        const operationType = operation.operationType;
        const layerType = operation.layerType || "USER";
        const payload = operation.payload || {};
        if (operationType === "CLEAR_LAYER") {
            if (layerType === "AI") {
                state.aiAnnotations = [];
            } else {
                state.canvasObjects = [];
            }
            return;
        }

        if (operationType === "ADD_OBJECT") {
            if (layerType === "AI") {
                state.aiAnnotations.push({
                    ...payload,
                    objectId: payload.objectId || ("replay-ai-op-" + operation.operationId),
                    source: "AI"
                });
            } else {
                state.canvasObjects.push(fromProtocolObject({
                    ...payload,
                    source: payload.source || "USER"
                }));
            }
            return;
        }

        if (operationType === "UPDATE_OBJECT") {
            if (Array.isArray(payload.objects)) {
                if (layerType === "AI") {
                    state.aiAnnotations = payload.objects;
                } else {
                    state.canvasObjects = payload.objects.map(fromProtocolObject);
                }
                return;
            }
            if (payload.objectId) {
                const objects = layerType === "AI" ? state.aiAnnotations : state.canvasObjects;
                const index = objects.findIndex(function (object) {
                    return object.objectId === payload.objectId;
                });
                if (index >= 0) {
                    objects[index] = layerType === "AI" ? payload : fromProtocolObject(payload);
                }
            }
            return;
        }

        if (operationType === "DELETE_OBJECT" && payload.objectId) {
            if (layerType === "AI") {
                state.aiAnnotations = state.aiAnnotations.filter(function (object) {
                    return object.objectId !== payload.objectId;
                });
            } else {
                state.canvasObjects = state.canvasObjects.filter(function (object) {
                    return object.objectId !== payload.objectId;
                });
            }
        }
    }

    function playReplay() {
        if (!state.replay.loaded) {
            loadReplay().then(function () {
                if (state.replay.loaded && state.replay.timeline.length) {
                    playReplay();
                }
            });
            return;
        }
        if (!state.replay.timeline.length) {
            showToast("当前会话没有可播放的回放事件。", true);
            return;
        }
        if (state.replay.playing) {
            stopReplayTimer();
            renderReplayTimeline();
            return;
        }

        state.replay.playing = true;
        dom.replayPlayBtn.textContent = "暂停";

        const tick = function () {
            if (!state.replay.playing) {
                return;
            }
            if (state.replay.stepIndex >= state.replay.timeline.length) {
                stopReplayTimer();
                renderReplayTimeline();
                return;
            }
            applyReplayToStep(state.replay.stepIndex + 1);
            state.replay.timer = setTimeout(tick, Number(dom.replaySpeedSelect.value || 1200));
        };
        tick();
    }

    async function exitReplayMode() {
        if (!state.currentSessionId) {
            resetReplayState(true);
            return;
        }
        stopReplayTimer();
        resetReplayState(true);
        await selectSession(state.currentSessionId);
        showToast("已退出回放模式");
    }

    function clearCurrentSessionView() {
        revokeCurrentImageObjectUrl();
        resetReplayState(true);
        state.currentSessionId = null;
        state.currentImageId = null;
        state.currentImageUrl = "";
        state.currentImageNaturalWidth = 0;
        state.currentImageNaturalHeight = 0;
        state.messages = [];
        state.aiAnnotations = [];
        state.canvasVersion = null;
        dom.sessionHeading.textContent = "当前未选择会话";
        dom.sessionMeta.textContent = "先登录、上传题图，再创建会话开始讲解。";
        dom.questionImage.removeAttribute("src");
        renderMessages();
        resetCanvasState();
        redrawCanvas();
        updateCanvasExport();
        renderReplayTimeline();
    }

    function revokeCurrentImageObjectUrl() {
        if (!state.currentImageObjectUrl) {
            return;
        }
        URL.revokeObjectURL(state.currentImageObjectUrl);
        state.currentImageObjectUrl = "";
    }

    function syncCanvasSize() {
        const width = dom.questionImage.clientWidth || 640;
        const height = dom.questionImage.clientHeight || 240;
        dom.annotationCanvas.width = width;
        dom.annotationCanvas.height = height;
        redrawCanvas();
    }

    function toCanvasX(rawX) {
        if (!state.currentImageNaturalWidth) {
            return Number(rawX || 0);
        }
        return (Number(rawX || 0) / state.currentImageNaturalWidth) * dom.annotationCanvas.width;
    }

    function toCanvasY(rawY) {
        if (!state.currentImageNaturalHeight) {
            return Number(rawY || 0);
        }
        return (Number(rawY || 0) / state.currentImageNaturalHeight) * dom.annotationCanvas.height;
    }

    function toCanvasWidth(rawWidth) {
        if (!state.currentImageNaturalWidth) {
            return Number(rawWidth || 0);
        }
        return (Number(rawWidth || 0) / state.currentImageNaturalWidth) * dom.annotationCanvas.width;
    }

    function toCanvasHeight(rawHeight) {
        if (!state.currentImageNaturalHeight) {
            return Number(rawHeight || 0);
        }
        return (Number(rawHeight || 0) / state.currentImageNaturalHeight) * dom.annotationCanvas.height;
    }

    function drawArrow(x, y, toX, toY, color, size, dashed) {
        const headLength = 12 + (size || 3);
        const angle = Math.atan2(toY - y, toX - x);
        canvasContext.save();
        canvasContext.strokeStyle = color;
        canvasContext.fillStyle = color;
        canvasContext.lineWidth = size || 3;
        if (dashed) {
            canvasContext.setLineDash([8, 6]);
        }
        canvasContext.beginPath();
        canvasContext.moveTo(x, y);
        canvasContext.lineTo(toX, toY);
        canvasContext.stroke();

        canvasContext.setLineDash([]);
        canvasContext.beginPath();
        canvasContext.moveTo(toX, toY);
        canvasContext.lineTo(
            toX - headLength * Math.cos(angle - Math.PI / 6),
            toY - headLength * Math.sin(angle - Math.PI / 6)
        );
        canvasContext.lineTo(
            toX - headLength * Math.cos(angle + Math.PI / 6),
            toY - headLength * Math.sin(angle + Math.PI / 6)
        );
        canvasContext.closePath();
        canvasContext.fill();
        canvasContext.restore();
    }

    function drawLabel(text, x, y, color) {
        if (!text) {
            return;
        }
        canvasContext.save();
        canvasContext.font = "14px sans-serif";
        const paddingX = 8;
        const boxHeight = 28;
        const metrics = canvasContext.measureText(text);
        const boxWidth = metrics.width + paddingX * 2;
        const boxY = Math.max(0, y - boxHeight - 6);
        canvasContext.fillStyle = "rgba(31, 28, 22, 0.84)";
        canvasContext.fillRect(x, boxY, boxWidth, boxHeight);
        canvasContext.fillStyle = color || "#ffffff";
        canvasContext.fillText(text, x + paddingX, boxY + 18);
        canvasContext.restore();
    }

    function drawAiAnnotation(annotation) {
        const type = String(annotation.type || "").toLowerCase();
        const style = annotation.style || {};
        const color = annotation.color || style.strokeColor || style.fillColor || "#1f6feb";
        const lineWidth = Number(annotation.size || style.strokeWidth || 2);

        if (type === "rect" || type === "highlight") {
            const x = toCanvasX(annotation.x);
            const y = toCanvasY(annotation.y);
            const width = toCanvasWidth(annotation.width);
            const height = toCanvasHeight(annotation.height);
            canvasContext.save();
            if (type === "highlight") {
                canvasContext.globalAlpha = Number(style.opacity || annotation.opacity || 1);
                canvasContext.fillStyle = annotation.fillColor || style.fillColor || "rgba(253, 224, 71, 0.28)";
                canvasContext.fillRect(x, y, width, height);
            } else {
                canvasContext.strokeStyle = color;
                canvasContext.lineWidth = lineWidth;
                canvasContext.setLineDash([10, 6]);
                canvasContext.strokeRect(x, y, width, height);
            }
            canvasContext.restore();
            drawLabel(annotation.label, x, y, color);
            return;
        }

        if (type === "arrow") {
            const x = toCanvasX(annotation.x);
            const y = toCanvasY(annotation.y);
            const toX = toCanvasX(annotation.toX);
            const toY = toCanvasY(annotation.toY);
            drawArrow(x, y, toX, toY, color, lineWidth, true);
            drawLabel(annotation.label, Math.min(x, toX), Math.min(y, toY), color);
            return;
        }

        if (type === "text") {
            const x = toCanvasX(annotation.x);
            const y = toCanvasY(annotation.y);
            canvasContext.save();
            canvasContext.fillStyle = color;
            canvasContext.font = "18px sans-serif";
            canvasContext.fillText(annotation.text || annotation.label || "AI 标注", x, y);
            canvasContext.restore();
        }
    }

    function drawUserObject(object) {
        canvasContext.save();
        canvasContext.lineWidth = object.size || state.canvasSize;
        canvasContext.strokeStyle = object.color || state.canvasColor;
        canvasContext.fillStyle = object.color || state.canvasColor;
        canvasContext.lineCap = "round";
        canvasContext.lineJoin = "round";

        if (object.type === "pen") {
            canvasContext.beginPath();
            (object.points || []).forEach(function (point, index) {
                if (index === 0) {
                    canvasContext.moveTo(point.x, point.y);
                } else {
                    canvasContext.lineTo(point.x, point.y);
                }
            });
            canvasContext.stroke();
        } else if (object.type === "rect") {
            canvasContext.strokeRect(object.x, object.y, object.width, object.height);
        } else if (object.type === "arrow") {
            drawArrow(
                object.x,
                object.y,
                object.toX,
                object.toY,
                object.color || state.canvasColor,
                object.size || state.canvasSize,
                false
            );
        } else if (object.type === "text") {
            canvasContext.font = "20px sans-serif";
            canvasContext.fillText(object.text || "文字", object.x, object.y);
        }

        canvasContext.restore();
    }

    function redrawCanvas() {
        canvasContext.clearRect(0, 0, dom.annotationCanvas.width, dom.annotationCanvas.height);
        state.aiAnnotations.forEach(drawAiAnnotation);
        state.canvasObjects.forEach(drawUserObject);
        if (state.drawing && state.drawing.preview) {
            drawUserObject(state.drawing.preview);
        }
        dom.canvasExport.textContent = JSON.stringify(buildCanvasExportPayload(), null, 2);
    }

    async function loadQuestionImage(path) {
        if (!path) {
            revokeCurrentImageObjectUrl();
            dom.questionImage.removeAttribute("src");
            state.currentImageNaturalWidth = 0;
            state.currentImageNaturalHeight = 0;
            redrawCanvas();
            return;
        }

        revokeCurrentImageObjectUrl();
        const response = await api(path, { method: "GET" });
        const blob = await response.blob();
        state.currentImageObjectUrl = URL.createObjectURL(blob);
        dom.questionImage.src = state.currentImageObjectUrl;
        await dom.questionImage.decode().catch(function () {
            throw new Error("题图解码失败");
        });
        state.currentImageNaturalWidth = dom.questionImage.naturalWidth || 0;
        state.currentImageNaturalHeight = dom.questionImage.naturalHeight || 0;
        syncCanvasSize();
    }

    async function selectSession(sessionId) {
        resetReplayState(true);
        state.currentSessionId = sessionId;
        renderSessionList();
        try {
            const responses = await Promise.all([
                api("/api/sessions/" + sessionId, { method: "GET" }),
                api("/api/sessions/" + sessionId + "/messages", { method: "GET" })
            ]);
            const detail = responses[0].data;
            const messages = responses[1].data || [];

            state.currentImageId = detail.image ? detail.image.imageId : null;
            state.currentImageUrl = detail.image ? detail.image.accessUrl : "";
            state.messages = messages;
            state.aiAnnotations = extractAnnotations(messages);

            dom.sessionHeading.textContent = detail.title || "未命名会话";
            dom.sessionMeta.textContent = [
                detail.modelCode,
                detail.subjectCode,
                detail.gradeLevel,
                formatDateTime(detail.updatedAt || detail.createdAt)
            ].filter(Boolean).join(" · ");

            renderMessages();
            await loadQuestionImage(state.currentImageUrl);
            await loadCanvasDocument();
            redrawCanvas();
        } catch (error) {
            showToast("会话详情加载失败：" + error.message, true);
        }
    }

    function getCanvasPoint(event) {
        const rect = dom.annotationCanvas.getBoundingClientRect();
        return {
            x: event.clientX - rect.left,
            y: event.clientY - rect.top
        };
    }

    function normalizeRect(preview) {
        const x = preview.width < 0 ? preview.x + preview.width : preview.x;
        const y = preview.height < 0 ? preview.y + preview.height : preview.y;
        return {
            ...preview,
            x: x,
            y: y,
            width: Math.abs(preview.width),
            height: Math.abs(preview.height)
        };
    }

    async function handleCanvasDown(event) {
        if (state.replay.active) {
            showToast("回放模式下不能编辑画布，请先退出回放。", true);
            return;
        }
        if (!state.currentImageUrl) {
            showToast("请先加载题图。", true);
            return;
        }

        const point = getCanvasPoint(event);
        if (state.canvasTool === "text") {
            const text = window.prompt("输入标注文字");
            if (text) {
                const newObject = {
                    objectId: nextObjectId("user-text"),
                    type: "text",
                    source: "USER",
                    x: point.x,
                    y: point.y,
                    text: text,
                    color: state.canvasColor,
                    size: state.canvasSize
                };
                state.canvasObjects.push(newObject);
                pushCanvasHistory();
                redrawCanvas();
                await appendCanvasOperation("ADD_OBJECT", "USER", toProtocolObject(newObject));
                await saveCanvasSnapshot();
            }
            return;
        }

        if (state.canvasTool === "pen") {
            state.drawing = {
                type: "pen",
                color: state.canvasColor,
                size: state.canvasSize,
                points: [point],
                preview: {
                    type: "pen",
                    color: state.canvasColor,
                    size: state.canvasSize,
                    points: [point]
                }
            };
            return;
        }

        state.drawing = {
            type: state.canvasTool,
            start: point,
            color: state.canvasColor,
            size: state.canvasSize,
            preview: null
        };
    }

    function handleCanvasMove(event) {
        if (!state.drawing) {
            return;
        }
        const point = getCanvasPoint(event);

        if (state.drawing.type === "pen") {
            state.drawing.points.push(point);
            state.drawing.preview = {
                type: "pen",
                color: state.drawing.color,
                size: state.drawing.size,
                points: state.drawing.points.slice()
            };
        } else if (state.drawing.type === "rect") {
            state.drawing.preview = {
                type: "rect",
                x: state.drawing.start.x,
                y: state.drawing.start.y,
                width: point.x - state.drawing.start.x,
                height: point.y - state.drawing.start.y,
                color: state.drawing.color,
                size: state.drawing.size
            };
        } else if (state.drawing.type === "arrow") {
            state.drawing.preview = {
                type: "arrow",
                x: state.drawing.start.x,
                y: state.drawing.start.y,
                toX: point.x,
                toY: point.y,
                color: state.drawing.color,
                size: state.drawing.size
            };
        }

        redrawCanvas();
    }

    async function handleCanvasUp() {
        if (!state.drawing) {
            return;
        }

        if (state.drawing.type === "pen" && state.drawing.points.length > 1) {
            state.canvasObjects.push({
                objectId: nextObjectId("user-path"),
                type: "pen",
                source: "USER",
                points: state.drawing.points.slice(),
                color: state.drawing.color,
                size: state.drawing.size
            });
        } else if (state.drawing.preview) {
            if (state.drawing.type === "rect") {
                state.canvasObjects.push({
                    ...normalizeRect(state.drawing.preview),
                    objectId: nextObjectId("user-rect"),
                    source: "USER"
                });
            } else {
                state.canvasObjects.push({
                    ...state.drawing.preview,
                    objectId: nextObjectId("user-" + state.drawing.type),
                    source: "USER"
                });
            }
        }

        state.drawing = null;
        pushCanvasHistory();
        redrawCanvas();
        const latestObject = state.canvasObjects[state.canvasObjects.length - 1];
        if (latestObject) {
            await appendCanvasOperation("ADD_OBJECT", "USER", toProtocolObject(latestObject));
        }
        await saveCanvasSnapshot();
    }

    function setTool(tool) {
        state.canvasTool = tool;
        dom.toolButtons.forEach(function (button) {
            button.classList.toggle("active", button.dataset.tool === tool);
        });
    }

    async function restoreLogin() {
        renderAuthStatus();
        if (!state.token) {
            navigateToAuthPage();
            return;
        }
        try {
            const result = await api("/api/auth/me", { method: "GET" });
            state.currentUser = result.data;
            renderAuthStatus();
            await Promise.all([loadModels(), loadSessions()]);
        } catch (error) {
            saveToken("");
            state.currentUser = null;
            renderAuthStatus();
            navigateToAuthPage();
        }
    }

    async function handleUpload(event) {
        event.preventDefault();
        if (!state.token) {
            navigateToAuthPage();
            return;
        }
        const file = dom.imageFileInput.files[0];
        if (!file) {
            showToast("请选择题图文件。", true);
            return;
        }

        try {
            const formData = new FormData();
            formData.append("file", file);
            const result = await api("/api/images/upload", {
                method: "POST",
                body: formData,
                headers: {}
            });
            state.currentImageId = result.data.imageId;
            state.currentImageUrl = result.data.accessUrl;
            dom.imageMeta.textContent = "已上传：" + result.data.fileName + " / imageId=" + result.data.imageId;
            await loadQuestionImage(result.data.accessUrl);
            showToast("题图上传成功");
        } catch (error) {
            showToast("题图上传失败：" + error.message, true);
        }
    }

    async function handleCreateSession(event) {
        event.preventDefault();
        if (!state.token || !state.currentImageId) {
            showToast("请先上传题图后再创建会话。", true);
            return;
        }
        if (!dom.modelSelect.value) {
            showToast("当前没有可用模型。", true);
            return;
        }

        try {
            const result = await api("/api/sessions", {
                method: "POST",
                body: JSON.stringify({
                    imageId: state.currentImageId,
                    modelCode: dom.modelSelect.value,
                    title: dom.sessionTitleInput.value,
                    subjectCode: dom.subjectCodeInput.value,
                    gradeLevel: dom.gradeLevelInput.value
                })
            });
            showToast("会话创建成功");
            await loadSessions();
            await selectSession(result.data.sessionId);
        } catch (error) {
            showToast("创建会话失败：" + error.message, true);
        }
    }

    async function readErrorMessage(response) {
        try {
            const contentType = response.headers.get("Content-Type") || "";
            if (contentType.includes("application/json")) {
                const payload = await response.json();
                return payload.message || payload.error || response.statusText || "请求失败";
            }
            const text = await response.text();
            return text || response.statusText || "请求失败";
        } catch (error) {
            return response.statusText || "请求失败";
        }
    }

    function removeDraftMessages(userDraft, assistantDraft) {
        state.messages = state.messages.filter(function (message) {
            return message !== userDraft && message !== assistantDraft;
        });
    }

    function replaceMessageDraft(draft, persistedMessage) {
        const draftIndex = state.messages.indexOf(draft);
        if (draftIndex >= 0 && persistedMessage) {
            state.messages.splice(draftIndex, 1, persistedMessage);
        }
    }

    async function sendMessageWithoutStream(content, drafts, mode) {
        if (drafts) {
            removeDraftMessages(drafts.userDraft, drafts.assistantDraft);
        }
        const result = await api("/api/sessions/" + state.currentSessionId + "/messages", {
            method: "POST",
            body: JSON.stringify({
                content: content,
                useStream: false,
                mode: mode
            })
        });
        state.messages.push(result.data.userMessage, result.data.assistantMessage);
        state.aiAnnotations = extractAnnotations(state.messages);
        renderMessages();
    }

    async function streamAssistantMessage(content, mode) {
        const now = new Date().toISOString();
        const userDraft = {
            messageId: "streaming-user-" + Date.now(),
            roleCode: "USER",
            contentText: content,
            annotationSummary: [],
            createdAt: now,
            streaming: true
        };
        const assistantDraft = {
            messageId: "streaming-assistant-" + Date.now(),
            roleCode: "ASSISTANT",
            contentText: "",
            hintLevel: null,
            guidanceStage: "streaming",
            teacherIntent: "guide_next_step",
            annotationSummary: [],
            createdAt: now,
            streaming: true
        };
        state.messages.push(userDraft, assistantDraft);
        renderMessages();

        let renderQueued = false;
        function scheduleStreamRender() {
            if (renderQueued) {
                return;
            }
            renderQueued = true;
            // 高频 delta 只在下一帧统一刷新，保证“实时”同时避免整页重绘抖动。
            window.requestAnimationFrame(function () {
                renderQueued = false;
                renderMessages();
            });
        }

        let response;
        try {
            response = await fetch("/api/sessions/" + state.currentSessionId + "/messages/stream", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json; charset=utf-8",
                    "Authorization": "Bearer " + state.token
                },
                body: JSON.stringify({
                    content: content,
                    useStream: true,
                    mode: mode
                })
            });
        } catch (error) {
            removeDraftMessages(userDraft, assistantDraft);
            renderMessages();
            throw error;
        }
        if (!response.ok || !response.body) {
            const message = await readErrorMessage(response);
            await sendMessageWithoutStream(content, { userDraft: userDraft, assistantDraft: assistantDraft }, mode);
            showToast("流式接口不可用，已自动切换为普通输出：" + response.status + " " + message, true);
            return { fallback: true };
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";
        let userPersisted = false;
        let doneReceived = false;

        function handleEvent(block) {
            const lines = block.split(/\r?\n/);
            let eventName = "message";
            const dataLines = [];
            lines.forEach(function (line) {
                if (line.startsWith("event:")) {
                    eventName = line.slice(6).trim();
                } else if (line.startsWith("data:")) {
                    dataLines.push(line.slice(5).trim());
                }
            });
            if (!dataLines.length) {
                return;
            }
            const payload = JSON.parse(dataLines.join("\n"));
            if (eventName === "user") {
                userPersisted = true;
                replaceMessageDraft(userDraft, payload);
            } else if (eventName === "delta") {
                assistantDraft.contentText += payload.text || "";
                scheduleStreamRender();
                return;
            } else if (eventName === "done") {
                doneReceived = true;
                assistantDraft.streaming = false;
                replaceMessageDraft(userDraft, payload.userMessage);
                replaceMessageDraft(assistantDraft, payload.assistantMessage);
                state.aiAnnotations = extractAnnotations(state.messages);
            } else if (eventName === "error") {
                throw new Error(payload.message || "流式生成失败");
            }
            renderMessages();
        }

        while (true) {
            const result = await reader.read();
            if (result.done) {
                break;
            }
            buffer += decoder.decode(result.value, { stream: true });
            const blocks = buffer.split(/\r?\n\r?\n/);
            buffer = blocks.pop() || "";
            blocks.forEach(handleEvent);
        }
        if (buffer.trim()) {
            handleEvent(buffer);
        }
        if (!doneReceived) {
            assistantDraft.streaming = false;
            assistantDraft.guidanceStage = "interrupted";
            if (!userPersisted) {
                userDraft.streaming = false;
            }
            renderMessages();
            throw new Error("流式响应提前结束，请稍后重试");
        }
        return { fallback: false };
    }

    async function handleSendMessage(event) {
        event.preventDefault();
        if (!state.currentSessionId) {
            showToast("请先创建或选择会话。", true);
            return;
        }

        const content = dom.messageInput.value.trim();
        if (!content) {
            showToast("请输入消息内容。", true);
            return;
        }
        if (state.sendingMessage) {
            showToast("上一条消息还在生成中，请稍等片刻。", true);
            return;
        }

        try {
            state.sendingMessage = true;
            if (state.replay.active) {
                await exitReplayMode();
            }
            dom.messageInput.value = "";
            const sendResult = await streamAssistantMessage(content, state.answerMode);
            renderMessages();
            redrawCanvas();
            await loadSessions();
            if (!sendResult || !sendResult.fallback) {
                showToast("消息发送成功");
            }
        } catch (error) {
            showToast("消息发送失败：" + error.message, true);
        } finally {
            state.sendingMessage = false;
        }
    }

    function bindEvents() {
        dom.uploadForm.addEventListener("submit", handleUpload);
        dom.createSessionForm.addEventListener("submit", handleCreateSession);
        dom.messageForm.addEventListener("submit", handleSendMessage);
        dom.answerModeSelect.value = state.answerMode;
        dom.answerModeSelect.addEventListener("change", function () {
            state.answerMode = dom.answerModeSelect.value === "direct" ? "direct" : "guided";
            localStorage.setItem(ANSWER_MODE_KEY, state.answerMode);
            dom.messageHint.textContent = state.answerMode === "direct"
                ? "直接识别题目并输出答案和完整解题过程。"
                : "先识别条件并提出一个小问题，之后根据你的回答逐步提示。";
        });
        dom.refreshSessionsBtn.addEventListener("click", loadSessions);
        dom.loadReplayBtn.addEventListener("click", loadReplay);
        dom.replayPlayBtn.addEventListener("click", playReplay);
        dom.replayPrevBtn.addEventListener("click", function () {
            stopReplayTimer();
            if (!state.replay.loaded) {
                return;
            }
            applyReplayToStep(state.replay.stepIndex - 1);
        });
        dom.replayNextBtn.addEventListener("click", function () {
            stopReplayTimer();
            if (!state.replay.loaded) {
                loadReplay();
                return;
            }
            applyReplayToStep(state.replay.stepIndex + 1);
        });
        dom.replayResetBtn.addEventListener("click", function () {
            stopReplayTimer();
            if (state.replay.loaded) {
                applyReplayToStep(0);
            }
        });
        dom.replayExitBtn.addEventListener("click", exitReplayMode);
        dom.logoutBtn.addEventListener("click", async function () {
            saveToken("");
            state.currentUser = null;
            state.sessions = [];
            clearCurrentSessionView();
            renderSessionList();
            renderAuthStatus();
            showToast("已退出登录");
            navigateToAuthPage();
        });

        dom.toolButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                setTool(button.dataset.tool);
            });
        });
        dom.colorInput.addEventListener("input", function (event) {
            state.canvasColor = event.target.value;
        });
        dom.sizeInput.addEventListener("input", function (event) {
            state.canvasSize = Number(event.target.value);
        });
        dom.clearCanvasBtn.addEventListener("click", async function () {
            if (state.replay.active) {
                showToast("回放模式下不能编辑画布，请先退出回放。", true);
                return;
            }
            resetCanvasState();
            redrawCanvas();
            await appendCanvasOperation("CLEAR_LAYER", "USER", { layerType: "USER" });
            await saveCanvasSnapshot();
        });
        dom.undoCanvasBtn.addEventListener("click", async function () {
            if (state.replay.active) {
                showToast("回放模式下不能编辑画布，请先退出回放。", true);
                return;
            }
            if (state.canvasHistory.length <= 1) {
                return;
            }
            state.canvasHistory.pop();
            state.canvasObjects = JSON.parse(state.canvasHistory[state.canvasHistory.length - 1] || "[]");
            redrawCanvas();
            await appendCanvasOperation("UPDATE_OBJECT", "USER", buildCanvasSnapshot().layers[1]);
            await saveCanvasSnapshot();
        });
        dom.exportCanvasBtn.addEventListener("click", async function () {
            try {
                await navigator.clipboard.writeText(dom.canvasExport.textContent);
                showToast("画布 JSON 已复制到剪贴板");
            } catch (error) {
                showToast("复制失败，请手动复制 JSON。", true);
            }
        });

        dom.annotationCanvas.addEventListener("pointerdown", handleCanvasDown);
        dom.annotationCanvas.addEventListener("pointermove", handleCanvasMove);
        dom.annotationCanvas.addEventListener("pointerup", handleCanvasUp);
        dom.annotationCanvas.addEventListener("pointerleave", handleCanvasUp);
        window.addEventListener("resize", syncCanvasSize);
        dom.questionImage.addEventListener("load", syncCanvasSize);
        dom.questionImage.addEventListener("error", function () {
            showToast("题图加载失败，请重新上传或刷新会话。", true);
        });
        window.addEventListener("beforeunload", revokeCurrentImageObjectUrl);
    }

    bindEvents();
    setTool("pen");
    renderAuthStatus();
    renderModelSelect();
    renderSessionList();
    renderMessages();
    resetCanvasState();
    updateCanvasExport();
    renderReplayTimeline();
    restoreLogin();
})();
