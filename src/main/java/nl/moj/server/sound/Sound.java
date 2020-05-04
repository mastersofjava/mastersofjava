/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.sound;

public enum Sound {

    TIC_TAC("slowtictac.wav"),
    SLOW_TIC_TAC("tictac2.wav"),
    FAST_TIC_TAC("tikking.wav"),
    GONG("gong.wav");

    private String filename;

    Sound(String filename) {
        this.filename = filename;
    }

    public String filename() {
        return filename;
    }
}
