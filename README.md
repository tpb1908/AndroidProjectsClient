# AndroidProjectsClient

An Android client for [GitHub projects](https://github.com/blog/2256-a-whole-new-github-universe-announcing-new-tools-forums-and-features).

Built using [the official GitHub API](https://developer.github.com/v3/projects/) and [Fast-Android-Networking](https://github.com/amitshekhariitbhu/Fast-Android-Networking).

### Dependencies

* [ButterKnife](https://github.com/JakeWharton/butterknife)

* [OkHttp](https://github.com/square/okhttp)

* [Fast-Android-Networking](https://github.com/amitshekhariitbhu/Fast-Android-Networking)

* [Clans FloatingActionButton](https://github.com/Clans/FloatingActionButton)

* [CWAC AndDown](https://github.com/commonsguy/cwac-anddown)

* [HtmlTextView](https://github.com/SufficientlySecure/html-textview)

* [MarkedView](https://github.com/mittsuu/MarkedView-for-Android)

* [CWAC Pager](https://github.com/commonsguy/cwac-pager)

* [AOSP support library](https://developer.android.com/tools/support-library/features.html)


### Features

* Secure sign in with OAUTH
* Creating projects on a repository
* Editing projects within a repository
* Deleting projects within a repository
* Viewing of projects columns and their cards
* Home screen shortcuts for users, repositories, and projects
* MarkDown support throughout
* Editing of column names
* Creation of new columns
* Deletion of columns
* Creation of new cards
* Creation of new issue cards from existing open issues (Not available on site)
* Creation of new issue cards (Not available on site)
* Conversion of cards to issues
* Tagged users displayed as links on card
* Issue labels displayed on issue cards
* Issue assignees displayed on issue cards
* Assignees formatted as links
* Editing of issues (Not available on site)
* Editing of issue collaborators (Not available on site)
* Collaborators displayed as links
* Editing of issue labels (Not available on site)
* Opening and closing of associated issues (Not available on site)
* Adding a comment when opening or closing issues
* Option to close issues when their card is removed (Not available on site)
* Fullscreen viewer for cards
* Drag and drop to move cards between columns
* Drag and drop to rearrange columns in a project
* Link handling to open users, repositories, and projects

### Upcoming features (In order of precedence)

* Sharing (Of columns and cards)
* Adding milestone when creating issue, and editing milestones when editing issues
* Comment viewer for issues, and the ability to reply to comments
* Nougat app shortcuts for projects/repos pinned by the user (this requires link/intent handling)
* Creating/Editing labels for a repository
* Extra column in project to display all issues, and allow creating cards from them
* Moving of columns between projects
* Moving of cards between projects
* Backup and restore
* Cache of changes
* Offline storage
* Notification service for new cards, issues etc

### Setup

**Clone of fork the repository**


**Register a new application on GitHub**

* Go to https://github.com/settings/developers

* Add you application name and callback url

* Add the client ID and secret to gradle.properties

```
GITHUB_CLIENT_ID="ID"

GITHUB_CLIENT_SECRET="SECRET"
```

**Add your information to gradle.properties**

```
REPO_ADDRESS="Address of your fork"
GITHUB_REDIRECT_URL="Your redirect url"
FEATURE_REQUEST_EMAIL="Your email"
BUG_EMAIL="Your email"
```

**Setup or remove analytics**

If you want to use FireBase analytics and crash reporting you need to register and add your google-services.json

The setup process can be completed through Android Studios' helper under tools -> FireBase.


### Screenshots
<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/repos.png" width="720" height="1280" />

<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/repo.png" width="720" height="1280" />

<br />

<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/project.png" width="720" height="1280" />

<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/editing.png" width="720" height="1280" />

<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/issue_conversion.png" width="720" height="1280" />

<img src="https://github.com/tpb1908/AndroidProjectsClient/blob/master/screenshots/assignment.png" width="720" height="1280" />
### License

    Copyright 2016 Theo Pearson-Bray

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.