/*
 * Copyright 2014 ParanoidAndroid Project
 *
 * This file is part of CypherOS OTA.
 *
 * CypherOS OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CypherOS OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CypherOS OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cypher.cota.updater;

import android.content.Context;

import com.cypher.cota.R;
import com.cypher.cota.updater.server.CypherServer;
import com.cypher.cota.utils.DeviceInfoUtils;
import com.cypher.cota.utils.Version;

public class RomUpdater extends Updater {

    public RomUpdater(Context context, boolean fromAlarm) {
        super(context, new Server[]{
                new CypherServer()}, fromAlarm);
    }

    @Override
    public Version getVersion() {
        return new Version(DeviceInfoUtils.getVersionString());
    }

    @Override
    public String getDevice() {
        return DeviceInfoUtils.getDevice();
    }

    @Override
    public int getErrorStringId() {
        return R.string.download_failed_title;
    }

}
