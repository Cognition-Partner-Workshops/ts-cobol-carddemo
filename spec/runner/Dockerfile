# GnuCOBOL runner for the batch path. Build from the repository root:
#   docker build -f spec/runner/Dockerfile -t carddemo-cobol-runner .
FROM public.ecr.aws/docker/library/ubuntu:22.04
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      gnucobol3 python3 python3-yaml python3-pytest \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /work
COPY app/cbl app/cbl
COPY app/cpy app/cpy
COPY app/jcl app/jcl
COPY app/data/ASCII app/data/ASCII
COPY spec spec
WORKDIR /work/spec
ENTRYPOINT ["python3", "-m", "harness"]
CMD ["all", "/data"]
