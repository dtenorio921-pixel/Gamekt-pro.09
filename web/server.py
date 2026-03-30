import http.server
import socketserver
import os
import zipfile
import io
import json
import subprocess
import urllib.request
import urllib.error

PORT = 5000
WEB_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(WEB_DIR)

MODIFIED_FILES = [
    ".github/workflows/build-apk.yml",
    "app/src/main/assets/box86_64/default.box64rc",
    "app/src/main/assets/box86_64/lightsteam.box64rc",
    "app/src/main/assets/box86_64/ultralightsteam.box64rc",
    "app/src/main/java/app/gamenative/PrefManager.kt",
    "app/src/main/java/app/gamenative/gamefixes/AutoGameOptimizer.kt",
    "app/src/main/java/app/gamenative/gamefixes/GameFixesRegistry.kt",
    "app/src/main/java/com/winlator/container/Container.java",
    "app/src/main/java/com/winlator/core/DefaultVersion.java",
]

def github_api(path, token, method="GET", data=None):
    url = f"https://api.github.com{path}"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
        "User-Agent": "gamekt-pro-pusher/1.0",
    }
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read()), resp.status
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        try:
            return json.loads(body), e.code
        except Exception:
            return {"message": body}, e.code

def push_to_github(token, repo_name):
    # 1. Get GitHub username
    user_data, status = github_api("/user", token)
    if status != 200:
        return False, f"Token inválido ou sem permissão. ({user_data.get('message', status)})"
    username = user_data["login"]

    # 2. Create repository (ignore error if already exists)
    create_data = {
        "name": repo_name,
        "description": "Gamekt pro — App Android otimizado para Moto G35",
        "private": False,
        "auto_init": False,
    }
    repo_data, status = github_api("/user/repos", token, method="POST", data=create_data)
    if status not in (201, 422):
        return False, f"Erro ao criar repositório: {repo_data.get('message', status)}"

    # 3. Push directly using the URL (no named remote needed, avoids git config lock issues)
    remote_url = f"https://{username}:{token}@github.com/{username}/{repo_name}.git"

    env = os.environ.copy()
    env["GIT_TERMINAL_PROMPT"] = "0"
    env["GIT_ASKPASS"] = "echo"

    result = subprocess.run(
        ["git", "push", remote_url, "master:master", "--force"],
        capture_output=True, text=True, cwd=ROOT_DIR, timeout=180, env=env
    )

    if result.returncode != 0:
        err = (result.stderr or result.stdout or "").strip()
        return False, f"Erro no push: {err[:400]}"

    actions_url = f"https://github.com/{username}/{repo_name}/actions"
    return True, actions_url


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def do_GET(self):
        if self.path == "/download-gamekt-pro.zip":
            self.send_zip()
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == "/api/push-to-github":
            self.handle_push()
        else:
            self.send_response(404)
            self.end_headers()

    def handle_push(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        try:
            payload = json.loads(body)
            token = payload.get("token", "").strip()
            repo  = payload.get("repo", "gamekt-pro").strip()
        except Exception:
            self.respond_json({"ok": False, "error": "Payload inválido."})
            return

        if not token:
            self.respond_json({"ok": False, "error": "Token não informado."})
            return

        ok, result = push_to_github(token, repo)
        if ok:
            self.respond_json({"ok": True, "actionsUrl": result})
        else:
            self.respond_json({"ok": False, "error": result})

    def respond_json(self, data):
        body = json.dumps(data).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def send_zip(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
            for rel_path in MODIFIED_FILES:
                abs_path = os.path.join(ROOT_DIR, rel_path)
                if os.path.exists(abs_path):
                    zf.write(abs_path, rel_path)
        data = buf.getvalue()
        self.send_response(200)
        self.send_header("Content-Type", "application/zip")
        self.send_header("Content-Disposition", 'attachment; filename="gamekt-pro-modificacoes.zip"')
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, format, *args):
        pass


print(f"Gamekt pro — Painel de Modificações rodando em http://0.0.0.0:{PORT}")
with socketserver.TCPServer(("0.0.0.0", PORT), Handler) as httpd:
    httpd.serve_forever()
