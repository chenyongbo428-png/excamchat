(function () {
    const TOKEN_KEY = "cheat.token";

    const state = {
        token: localStorage.getItem(TOKEN_KEY) || ""
    };

    const dom = {
        loginForm: document.getElementById("loginForm"),
        registerForm: document.getElementById("registerForm"),
        authStatus: document.getElementById("authStatus"),
        toast: document.getElementById("toast")
    };

    function showToast(message, isError) {
        dom.toast.textContent = message;
        dom.toast.classList.remove("hidden");
        dom.toast.style.background = isError ? "rgba(161, 42, 42, 0.94)" : "rgba(31, 28, 22, 0.92)";
        clearTimeout(showToast.timer);
        showToast.timer = setTimeout(function () {
            dom.toast.classList.add("hidden");
        }, 2600);
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

    function renderAuthStatus(text) {
        if (!dom.authStatus) {
            return;
        }
        if (text) {
            dom.authStatus.textContent = text;
            return;
        }
        dom.authStatus.textContent = state.token ? "检测到本地登录态，正在恢复..." : "未登录";
    }

    function navigateToWorkspace() {
        window.location.replace("/workspace.html");
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

        return response.json();
    }

    async function restoreLogin() {
        renderAuthStatus();
        if (!state.token) {
            return;
        }
        try {
            const result = await api("/api/auth/me", { method: "GET" });
            const user = result.data;
            const nickname = user.nickname || user.username;
            renderAuthStatus("已恢复登录：" + nickname + "（" + user.username + "）");
            navigateToWorkspace();
        } catch (error) {
            saveToken("");
            renderAuthStatus("登录态已失效，请重新登录");
        }
    }

    async function handleLogin(event) {
        event.preventDefault();
        const formData = new FormData(dom.loginForm);
        try {
            const result = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    username: formData.get("username"),
                    password: formData.get("password")
                })
            });
            saveToken(result.data.accessToken);
            renderAuthStatus("登录成功，正在进入工作台...");
            navigateToWorkspace();
        } catch (error) {
            showToast("登录失败：" + error.message, true);
        }
    }

    async function handleRegister(event) {
        event.preventDefault();
        const formData = new FormData(dom.registerForm);
        try {
            await api("/api/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    username: formData.get("username"),
                    password: formData.get("password"),
                    nickname: formData.get("nickname")
                })
            });
            dom.registerForm.reset();
            showToast("注册成功，请继续登录");
        } catch (error) {
            showToast("注册失败：" + error.message, true);
        }
    }

    function bindEvents() {
        dom.loginForm.addEventListener("submit", handleLogin);
        dom.registerForm.addEventListener("submit", handleRegister);
    }

    bindEvents();
    renderAuthStatus();
    restoreLogin();
})();
