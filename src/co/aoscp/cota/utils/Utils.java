/*
 * Copyright (C) 2018 CypherOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package co.aoscp.cota.utils;

import android.content.Intent;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;

import com.android.setupwizardlib.util.WizardManagerHelper;

import co.aoscp.cota.R;

public class Utils {

    @VisibleForTesting
    static final String SYSTEM_PROP_SETUPWIZARD_THEME = "setupwizard.theme";

    public static int getTheme(Intent intent) {
        String theme = intent.getStringExtra(WizardManagerHelper.EXTRA_THEME);
        if (theme == null) {
            theme = SystemProperties.get(SYSTEM_PROP_SETUPWIZARD_THEME);
        }
        if (theme != null) {
            switch (theme) {
                case WizardManagerHelper.THEME_GLIF_V2_LIGHT:
                    return R.style.GlifV2Theme_Light;
                case WizardManagerHelper.THEME_GLIF_V2:
                    return R.style.GlifV2Theme;
                case WizardManagerHelper.THEME_GLIF_LIGHT:
                    return R.style.GlifTheme_Light;
                case WizardManagerHelper.THEME_GLIF:
                    return R.style.GlifTheme;
            }
        }
        return R.style.GlifTheme_Light;
    }
}
