import logging
import os
import platform

import pandoc
from flask import Flask, Response, request
from gevent.pywsgi import WSGIServer

MIME_TYPES = {
    "html": "text/html",
    "html5": "text/html",
    "pdf": "application/pdf",
    "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "odt": "application/vnd.oasis.opendocument.text",
    "epub": "application/epub+zip",
    "markdown": "text/markdown",
    "md": "text/markdown",
    "latex": "application/x-latex",
    "tex": "application/x-tex",
    "rtf": "application/rtf",
    "txt": "text/plain",
    "json": "application/json",
    "xml": "application/xml",
}

DEFAULT_MIME_TYPE = "application/octet-stream"

FILE_EXTENSIONS = {
    "html": "html",
    "html5": "html",
    "pdf": "pdf",
    "docx": "docx",
    "odt": "odt",
    "epub": "epub",
    "markdown": "md",
    "md": "md",
    "latex": "tex",
    "tex": "tex",
    "rtf": "rtf",
    "txt": "txt",
    "json": "json",
    "xml": "xml",
    "asciidoc": "adoc",
    "rst": "rst",
    "org": "org",
    "revealjs": "html",
    "beamer": "pdf",
    "context": "tex",
    "textile": "textile",
    "dokuwiki": "txt",
    "mediawiki": "wiki",
    "man": "man",
    "ms": "ms",
    "pptx": "pptx",
    "plain": "txt",
}

app = Flask(__name__)


@app.route("/version", methods=["GET"])
def version():
    pandoc_config = pandoc.configure(auto=True, read=True)
    return {
        "python": platform.python_version(),
        "pandoc": pandoc_config.get("version"),
        "pandocService": os.environ.get("PANDOC_SERVICE_VERSION"),
        "timestamp": os.environ.get("PANDOC_SERVICE_BUILD_TIMESTAMP"),
    }


@app.route("/convert/<source_format>/to/<target_format>", methods=["POST"])
def convert(source_format, target_format):
    try:
        encoding = request.args.get("encoding", default="utf-8")
        file_name = request.args.get("file_name", default=("converted-document." + FILE_EXTENSIONS.get(target_format, "doc")))

        source = request.get_data().decode(encoding)
        doc = pandoc.read(source, format=source_format)
        output = pandoc.write(doc, format=target_format)

        pandoc_config = pandoc.configure(auto=True, read=True)
        mime_type = MIME_TYPES.get(target_format, DEFAULT_MIME_TYPE)

        response = Response(output, mimetype=mime_type, status=200)
        response.headers.add("Content-Disposition", "attachment; filename=" + file_name)
        response.headers.add("Python-Version", platform.python_version())
        response.headers.add("Pandoc-Version", pandoc_config.get("version"))
        response.headers.add("Pandoc-Service-Version", os.environ.get("PANDOC_SERVICE_VERSION"))
        return response

    except AssertionError as e:
        return process_error(e, "Assertion error, check the request body html", 400)
    except (UnicodeDecodeError, LookupError) as e:
        return process_error(e, "Cannot decode request html body", 400)
    except Exception as e:
        return process_error(e, "Unexpected error due converting to PDF", 500)


def process_error(e, err_msg, status):
    logging.exception(msg=err_msg + ": " + str(e))
    return Response(err_msg + ": " + getattr(e, "message", repr(e)), mimetype="plain/text", status=status)


def start_server(port):
    http_server = WSGIServer(("", port), app)
    http_server.serve_forever()
