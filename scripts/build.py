"""Build index.html from src/app.html: inlines the logo/icon and stamps the build id.
Usage: python3 scripts/build.py [BUILD_ID]"""
import base64, json, sys, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
build = sys.argv[1] if len(sys.argv) > 1 else "__BUILD__"
b64 = lambda p, mime: f"data:{mime};base64," + base64.b64encode((root / p).read_bytes()).decode()
html = (root / "src/app.html").read_text()
html = html.replace("__LOGO__", b64("icons/logo.jpg", "image/jpeg")).replace("__ICON__", b64("icons/icon-192.png", "image/png")).replace("__BUILD__", build)
(root / "index.html").write_text(html)
sw = (root / "src/sw.js").read_text().replace("__BUILD__", build)
(root / "sw.js").write_text(sw)
(root / "version.json").write_text(json.dumps({"build": build}) + "\n")
print("built", build)
