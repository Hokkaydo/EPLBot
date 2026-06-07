# syntax=docker/dockerfile:1
FROM python:3.9-slim
WORKDIR /usr/src/app
COPY src/main/java/com/github/hokkaydo/eplbot/module/code/python/requirements.txt ./
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt
CMD ["python", "-"]
