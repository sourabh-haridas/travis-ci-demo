FROM open-liberty
COPY --chown=1001:0  ./target/springboot.war /config/dropins/
