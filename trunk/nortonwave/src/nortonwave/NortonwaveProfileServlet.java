/* Profile details for Nortonwave */
package nortonwave;

import com.google.wave.api.ProfileServlet;

@SuppressWarnings("serial")

public class NortonwaveProfileServlet extends ProfileServlet{

        //Avatar
        @Override
        public String getRobotAvatarUrl() {
                return "http://nortonwave.appspot.com/nortonlogo.png";
        }

        //Name
        @Override
        public String getRobotName() {
                return "Norton SafeWave";
        }

        //URL
        @Override
        public String getRobotProfilePageUrl() {
                return "http://nortonwave.appspot.com/index.html";
        }


}
