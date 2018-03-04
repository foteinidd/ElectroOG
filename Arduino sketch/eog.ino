void setup() {
  Serial.begin(9600);
  pinMode(A0, INPUT);
}

void loop() {
  if (Serial.available() > 0) {
    char c = (char) Serial.read();
    if (c == 'g') {
        Serial.print('g');
        Serial.print(analogRead(A0));
        Serial.print('g');
    }
  }
}
