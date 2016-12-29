/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;


/**
 * Created by theo on 26/12/16.
 */

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private Preferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = SettingsActivity.Preferences.getPreferences(this);
        setTheme(preferences.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_settings);
        ((TextView) findViewById(R.id.text_version_number)).setText(BuildConfig.VERSION_NAME);
        ((Switch) findViewById(R.id.switch_dark_theme)).setChecked(preferences.isDarkThemeEnabled());
        ((Switch) findViewById(R.id.switch_enable_analytics)).setChecked(preferences.areAnalyticsEnabled());

        final Spinner spinner = (Spinner) findViewById(R.id.spinner_card_click);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.settings_card_actions));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                preferences.setCardAction(Preferences.CardAction.fromInt(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(preferences.getCardAction().toInt());

        Log.i(TAG, "onCreate: Emails " + BuildConfig.BUG_EMAIL + ", " + BuildConfig.FEATURE_REQUEST_EMAIL);
    }

    public void onSettingsClick(View view) {
        switch(view.getId()) {
            case R.id.switch_dark_theme:
                Log.i(TAG, "onSettingsClick: Toggle dark theme");
                preferences.setDarkThemeEnabled(((Switch) view).isChecked());
                break;
            case R.id.switch_enable_analytics:
                Log.i(TAG, "onSettingsClick: Toggle analytics");
                preferences.setAnalyticsEnabled(((Switch) view).isChecked());
                break;
            case R.id.layout_settings_version:
                Log.i(TAG, "onSettingsClick: Display version");
                break;
            case R.id.layout_settings_changelog:
                Log.i(TAG, "onSettingsClick: Display changelog");
                break;
            case R.id.layout_settings_licenses:
                Log.i(TAG, "onSettingsClick: Display licenses");
                new AlertDialog.Builder(this)
                        .setTitle("Licenses")
                        .setMessage(LICENSES)
                        .create().show();
                break;
            case R.id.layout_settings_repository:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.REPO_ADDRESS)));
                break;
            case R.id.layout_settings_developer:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tpb1908")));
                break;
            case R.id.layout_settings_bug_report:
                final Intent bugIntent = new Intent(Intent.ACTION_SEND);
                bugIntent.setType("text/email");
                bugIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.BUG_EMAIL});
                bugIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Projects bug report");
                bugIntent.putExtra(Intent.EXTRA_TEXT, "");
                startActivity(Intent.createChooser(bugIntent, "Send email:"));
                break;
            case R.id.layout_settings_feature_request:
                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/email");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.FEATURE_REQUEST_EMAIL});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Projects feature request");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Bug:\n\n\nSteps to reproduce:");
                startActivity(Intent.createChooser(emailIntent, "Send email:"));
                break;
        }
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

    public static class Preferences {
        private static final String TAG = Preferences.class.getSimpleName();
        private static Preferences preferences;
        private static final String KEY = "PREFS";

        private static final String KEY_DARK_THEME = "DT";
        private static final String KEY_ANALYTICS = "A";
        private static final String KEY_CARD_ACTION = "C";

        private SharedPreferences prefs;

        private Preferences(Context context) {
            prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        }

        public static Preferences getPreferences(Context context) {
            if(preferences == null) preferences = new Preferences(context);
            return preferences;
        }

        public boolean isDarkThemeEnabled() {
            return prefs.getBoolean(KEY_DARK_THEME, true);
        }

        public boolean areAnalyticsEnabled() {
            return prefs.getBoolean(KEY_ANALYTICS, true);
        }

        public CardAction getCardAction() {
            return CardAction.fromInt(prefs.getInt(KEY_CARD_ACTION, 0));
        }

        public void setDarkThemeEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
        }

        public void setAnalyticsEnabled(boolean enabled) {
            prefs.edit().putBoolean(KEY_ANALYTICS, enabled).apply();
        }

        public void setCardAction(CardAction action) {
            prefs.edit().putInt(KEY_CARD_ACTION, action.toInt()).apply();
        }

        public enum CardAction {
            EDIT, FULLSCREEN, COPY;

            int toInt() {
                switch(this) {
                    case EDIT:
                        return 0;
                    case FULLSCREEN:
                        return 1;
                    case COPY:
                        return 2;
                }
                return -1;
            }

            static CardAction fromInt(int i) {
                switch(i) {
                    case 0:
                        return EDIT;
                    case 1:
                        return FULLSCREEN;
                    case 2:
                        return COPY;
                    default:
                        return EDIT;
                }
            }
        }

    }

    final String LICENSES =
            "Apache License 2.0\n" +
                    "applies to:\n" +
                    "\n" +
                    "ButterKnife\nhttps://github.com/JakeWharton/butterknife\n" +
                    "\n" +
                    "OkHttp\nhttps://github.com/square/okhttp\n" +
                    "\n" +
                    "Fast-Android-Networking\nhttps://github.com/amitshekhariitbhu/Fast-Android-Networking\n" +
                    "    -Copyright (C) 2011 Android Open Source Project\n" +
                    "\n" +
                    "Android-Iconics\nhttps://github.com/mikepenz/Android-Iconics\n" +
                    "\n" +
                    "Clans FloatingActionButton\nhttps://github.com/Clans/FloatingActionButton\n" +
                    "\n" +
                    "CWAC AndDown\nhttps://github.com/commonsguy/cwac-anddown\n" +
                    "    hoedown https://github.com/hoedown/hoedown https://github.com/hoedown/hoedown/blob/master/LICENSE\n" +
                    "\n" +
                    "HtmlTextView\nhttps://github.com/SufficientlySecure/html-textview\n" +
                    "\n" +
                    "CWAC Pager\nhttps://github.com/commonsguy/cwac-pager\n" +
                    "\n" +
                    "\n" +
                    "                                 Apache License\n" +
                    "                           Version 2.0, January 2004\n" +
                    "                        http://www.apache.org/licenses/\n" +
                    "\n" +
                    "   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION\n" +
                    "\n" +
                    "   1. Definitions.\n" +
                    "\n" +
                    "      \"License\" shall mean the terms and conditions for use, reproduction,\n" +
                    "      and distribution as defined by Sections 1 through 9 of this document.\n" +
                    "\n" +
                    "      \"Licensor\" shall mean the copyright owner or entity authorized by\n" +
                    "      the copyright owner that is granting the License.\n" +
                    "\n" +
                    "      \"Legal Entity\" shall mean the union of the acting entity and all\n" +
                    "      other entities that control, are controlled by, or are under common\n" +
                    "      control with that entity. For the purposes of this definition,\n" +
                    "      \"control\" means (i) the power, direct or indirect, to cause the\n" +
                    "      direction or management of such entity, whether by contract or\n" +
                    "      otherwise, or (ii) ownership of fifty percent (50%) or more of the\n" +
                    "      outstanding shares, or (iii) beneficial ownership of such entity.\n" +
                    "\n" +
                    "      \"You\" (or \"Your\") shall mean an individual or Legal Entity\n" +
                    "      exercising permissions granted by this License.\n" +
                    "\n" +
                    "      \"Source\" form shall mean the preferred form for making modifications,\n" +
                    "      including but not limited to software source code, documentation\n" +
                    "      source, and configuration files.\n" +
                    "\n" +
                    "      \"Object\" form shall mean any form resulting from mechanical\n" +
                    "      transformation or translation of a Source form, including but\n" +
                    "      not limited to compiled object code, generated documentation,\n" +
                    "      and conversions to other media types.\n" +
                    "\n" +
                    "      \"Work\" shall mean the work of authorship, whether in Source or\n" +
                    "      Object form, made available under the License, as indicated by a\n" +
                    "      copyright notice that is included in or attached to the work\n" +
                    "      (an example is provided in the Appendix below).\n" +
                    "\n" +
                    "      \"Derivative Works\" shall mean any work, whether in Source or Object\n" +
                    "      form, that is based on (or derived from) the Work and for which the\n" +
                    "      editorial revisions, annotations, elaborations, or other modifications\n" +
                    "      represent, as a whole, an original work of authorship. For the purposes\n" +
                    "      of this License, Derivative Works shall not include works that remain\n" +
                    "      separable from, or merely link (or bind by name) to the interfaces of,\n" +
                    "      the Work and Derivative Works thereof.\n" +
                    "\n" +
                    "      \"Contribution\" shall mean any work of authorship, including\n" +
                    "      the original version of the Work and any modifications or additions\n" +
                    "      to that Work or Derivative Works thereof, that is intentionally\n" +
                    "      submitted to Licensor for inclusion in the Work by the copyright owner\n" +
                    "      or by an individual or Legal Entity authorized to submit on behalf of\n" +
                    "      the copyright owner. For the purposes of this definition, \"submitted\"\n" +
                    "      means any form of electronic, verbal, or written communication sent\n" +
                    "      to the Licensor or its representatives, including but not limited to\n" +
                    "      communication on electronic mailing lists, source code control systems,\n" +
                    "      and issue tracking systems that are managed by, or on behalf of, the\n" +
                    "      Licensor for the purpose of discussing and improving the Work, but\n" +
                    "      excluding communication that is conspicuously marked or otherwise\n" +
                    "      designated in writing by the copyright owner as \"Not a Contribution.\"\n" +
                    "\n" +
                    "      \"Contributor\" shall mean Licensor and any individual or Legal Entity\n" +
                    "      on behalf of whom a Contribution has been received by Licensor and\n" +
                    "      subsequently incorporated within the Work.\n" +
                    "\n" +
                    "   2. Grant of Copyright License. Subject to the terms and conditions of\n" +
                    "      this License, each Contributor hereby grants to You a perpetual,\n" +
                    "      worldwide, non-exclusive, no-charge, royalty-free, irrevocable\n" +
                    "      copyright license to reproduce, prepare Derivative Works of,\n" +
                    "      publicly display, publicly perform, sublicense, and distribute the\n" +
                    "      Work and such Derivative Works in Source or Object form.\n" +
                    "\n" +
                    "   3. Grant of Patent License. Subject to the terms and conditions of\n" +
                    "      this License, each Contributor hereby grants to You a perpetual,\n" +
                    "      worldwide, non-exclusive, no-charge, royalty-free, irrevocable\n" +
                    "      (except as stated in this section) patent license to make, have made,\n" +
                    "      use, offer to sell, sell, import, and otherwise transfer the Work,\n" +
                    "      where such license applies only to those patent claims licensable\n" +
                    "      by such Contributor that are necessarily infringed by their\n" +
                    "      Contribution(s) alone or by combination of their Contribution(s)\n" +
                    "      with the Work to which such Contribution(s) was submitted. If You\n" +
                    "      institute patent litigation against any entity (including a\n" +
                    "      cross-claim or counterclaim in a lawsuit) alleging that the Work\n" +
                    "      or a Contribution incorporated within the Work constitutes direct\n" +
                    "      or contributory patent infringement, then any patent licenses\n" +
                    "      granted to You under this License for that Work shall terminate\n" +
                    "      as of the date such litigation is filed.\n" +
                    "\n" +
                    "   4. Redistribution. You may reproduce and distribute copies of the\n" +
                    "      Work or Derivative Works thereof in any medium, with or without\n" +
                    "      modifications, and in Source or Object form, provided that You\n" +
                    "      meet the following conditions:\n" +
                    "\n" +
                    "      (a) You must give any other recipients of the Work or\n" +
                    "          Derivative Works a copy of this License; and\n" +
                    "\n" +
                    "      (b) You must cause any modified files to carry prominent notices\n" +
                    "          stating that You changed the files; and\n" +
                    "\n" +
                    "      (c) You must retain, in the Source form of any Derivative Works\n" +
                    "          that You distribute, all copyright, patent, trademark, and\n" +
                    "          attribution notices from the Source form of the Work,\n" +
                    "          excluding those notices that do not pertain to any part of\n" +
                    "          the Derivative Works; and\n" +
                    "\n" +
                    "      (d) If the Work includes a \"NOTICE\" text file as part of its\n" +
                    "          distribution, then any Derivative Works that You distribute must\n" +
                    "          include a readable copy of the attribution notices contained\n" +
                    "          within such NOTICE file, excluding those notices that do not\n" +
                    "          pertain to any part of the Derivative Works, in at least one\n" +
                    "          of the following places: within a NOTICE text file distributed\n" +
                    "          as part of the Derivative Works; within the Source form or\n" +
                    "          documentation, if provided along with the Derivative Works; or,\n" +
                    "          within a display generated by the Derivative Works, if and\n" +
                    "          wherever such third-party notices normally appear. The contents\n" +
                    "          of the NOTICE file are for informational purposes only and\n" +
                    "          do not modify the License. You may add Your own attribution\n" +
                    "          notices within Derivative Works that You distribute, alongside\n" +
                    "          or as an addendum to the NOTICE text from the Work, provided\n" +
                    "          that such additional attribution notices cannot be construed\n" +
                    "          as modifying the License.\n" +
                    "\n" +
                    "      You may add Your own copyright statement to Your modifications and\n" +
                    "      may provide additional or different license terms and conditions\n" +
                    "      for use, reproduction, or distribution of Your modifications, or\n" +
                    "      for any such Derivative Works as a whole, provided Your use,\n" +
                    "      reproduction, and distribution of the Work otherwise complies with\n" +
                    "      the conditions stated in this License.\n" +
                    "\n" +
                    "   5. Submission of Contributions. Unless You explicitly state otherwise,\n" +
                    "      any Contribution intentionally submitted for inclusion in the Work\n" +
                    "      by You to the Licensor shall be under the terms and conditions of\n" +
                    "      this License, without any additional terms or conditions.\n" +
                    "      Notwithstanding the above, nothing herein shall supersede or modify\n" +
                    "      the terms of any separate license agreement you may have executed\n" +
                    "      with Licensor regarding such Contributions.\n" +
                    "\n" +
                    "   6. Trademarks. This License does not grant permission to use the trade\n" +
                    "      names, trademarks, service marks, or product names of the Licensor,\n" +
                    "      except as required for reasonable and customary use in describing the\n" +
                    "      origin of the Work and reproducing the content of the NOTICE file.\n" +
                    "\n" +
                    "   7. Disclaimer of Warranty. Unless required by applicable law or\n" +
                    "      agreed to in writing, Licensor provides the Work (and each\n" +
                    "      Contributor provides its Contributions) on an \"AS IS\" BASIS,\n" +
                    "      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or\n" +
                    "      implied, including, without limitation, any warranties or conditions\n" +
                    "      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A\n" +
                    "      PARTICULAR PURPOSE. You are solely responsible for determining the\n" +
                    "      appropriateness of using or redistributing the Work and assume any\n" +
                    "      risks associated with Your exercise of permissions under this License.\n" +
                    "\n" +
                    "   8. Limitation of Liability. In no event and under no legal theory,\n" +
                    "      whether in tort (including negligence), contract, or otherwise,\n" +
                    "      unless required by applicable law (such as deliberate and grossly\n" +
                    "      negligent acts) or agreed to in writing, shall any Contributor be\n" +
                    "      liable to You for damages, including any direct, indirect, special,\n" +
                    "      incidental, or consequential damages of any character arising as a\n" +
                    "      result of this License or out of the use or inability to use the\n" +
                    "      Work (including but not limited to damages for loss of goodwill,\n" +
                    "      work stoppage, computer failure or malfunction, or any and all\n" +
                    "      other commercial damages or losses), even if such Contributor\n" +
                    "      has been advised of the possibility of such damages.\n" +
                    "\n" +
                    "   9. Accepting Warranty or Additional Liability. While redistributing\n" +
                    "      the Work or Derivative Works thereof, You may choose to offer,\n" +
                    "      and charge a fee for, acceptance of support, warranty, indemnity,\n" +
                    "      or other liability obligations and/or rights consistent with this\n" +
                    "      License. However, in accepting such obligations, You may act only\n" +
                    "      on Your own behalf and on Your sole responsibility, not on behalf\n" +
                    "      of any other Contributor, and only if You agree to indemnify,\n" +
                    "      defend, and hold each Contributor harmless for any liability\n" +
                    "      incurred by, or claims asserted against, such Contributor by reason\n" +
                    "      of your accepting any such warranty or additional liability.\n" +
                    "\n" +
                    "   END OF TERMS AND CONDITIONS\n" +
                    "\n" +
                    "   APPENDIX: How to apply the Apache License to your work.\n" +
                    "\n" +
                    "      To apply the Apache License to your work, attach the following\n" +
                    "      boilerplate notice, with the fields enclosed by brackets \"{}\"\n" +
                    "      replaced with your own identifying information. (Don't include\n" +
                    "      the brackets!)  The text should be enclosed in the appropriate\n" +
                    "      comment syntax for the file format. We also recommend that a\n" +
                    "      file or class name and description of purpose be included on the\n" +
                    "      same \"printed page\" as the copyright notice for easier\n" +
                    "      identification within third-party archives.\n" +
                    "\n" +
                    "   Copyright {yyyy} {name of copyright owner}\n" +
                    "\n" +
                    "   Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "   you may not use this file except in compliance with the License.\n" +
                    "   You may obtain a copy of the License at\n" +
                    "\n" +
                    "       http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "   Unless required by applicable law or agreed to in writing, software\n" +
                    "   distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "   See the License for the specific language governing permissions and\n" +
                    "   limitations under the License.\n" +
                    "\n" +
                    "\n" +
                    "MIT License\n" +
                    "applies to:\n" +
                    "\n" +
                    "MarkedView\nhttps://github.com/mittsuu/MarkedView-for-Android\n" +
                    "\n" +
                    "The MIT License (MIT)\n" +
                    "\n" +
                    "Copyright (c) 2016 mittsu\n" +
                    "\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                    "of this software and associated documentation files (the \"Software\"), to deal\n" +
                    "in the Software without restriction, including without limitation the rights\n" +
                    "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                    "copies of the Software, and to permit persons to whom the Software is\n" +
                    "furnished to do so, subject to the following conditions:\n" +
                    "\n" +
                    "The above copyright notice and this permission notice shall be included in all\n" +
                    "copies or substantial portions of the Software.\n" +
                    "\n" +
                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                    "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                    "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                    "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                    "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                    "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
                    "SOFTWARE.\n" +
                    "Contact GitHub API Training Shop Blog About\n";

}
