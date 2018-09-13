FROM ubuntu:latest

ADD script.sh /

RUN chmod +x /script.sh && bash -c "/script.sh"