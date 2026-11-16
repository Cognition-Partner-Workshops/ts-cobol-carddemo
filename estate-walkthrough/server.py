"""Serve the estate walkthrough and the mftb store it reads (stdlib only)."""
import argparse
import functools
import http.server
from pathlib import Path

appDir = Path(__file__).resolve().parent
storeDir = appDir.parent / "store"


class WalkthroughHandler(http.server.SimpleHTTPRequestHandler):
    def translate_path(self, path):
        cleanPath = path.split("?", 1)[0].split("#", 1)[0]
        if cleanPath.startswith("/store/"):
            target = (storeDir / cleanPath[len("/store/"):]).resolve()
            if storeDir in target.parents or target == storeDir:
                return str(target)
            return str(storeDir / "__missing__")
        return super().translate_path(path)

    def end_headers(self):
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8401)
    args = parser.parse_args()
    handler = functools.partial(WalkthroughHandler, directory=str(appDir / "static"))
    http.server.ThreadingHTTPServer(("0.0.0.0", args.port), handler).serve_forever()


if __name__ == "__main__":
    main()
