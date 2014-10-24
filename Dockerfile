# Nxt docker image
# for pulling from source we need public repo or credentials set
# start off with standard ubuntu images
#
# to use:
# docker build -t nxt .
# docker run -d --name nxtrun nxt
# docker logs nxtrun

FROM phusion/baseimage

#MAINTAINER 

#java7
RUN sed 's/main$/main universe/' -i /etc/apt/sources.list
RUN apt-get update && apt-get install -y software-properties-common python-software-properties
RUN add-apt-repository ppa:webupd8team/java -y
RUN apt-get update
RUN apt-get install -y wget unzip
RUN echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java7-installer

# (gradle2.0) not used
#RUN wget https://services.gradle.org/distributions/gradle-2.1-bin.zip
#RUN unzip -q gradle-2.1-bin.zip -d /usr/local/
#RUN echo "export GRADLE_HOME=/usr/local/gradle-2.1" >> $HOME/.bashrc
#RUN echo "export PATH=$PATH:/usr/local/gradle-2.1/bin" >> $HOME/.bashrc
#RUN export PATH=$PATH:/usr/local/gradle-2.1/bin
#RUN /usr/locaal/gradle-2.1/bin/gradle -version

# run and compile nxt
RUN mkdir /nxt
ADD . /nxt
ADD start.sh /start.sh
RUN chmod +x /start.sh

RUN cd /nxt; ./compile.sh
EXPOSE 7876 7874
#EXPOSE 7874
CMD ["/start.sh"]
