!function (exports, $, undefined) {
    var MApkTool = function () {
        var ApkToolApi = {}; // public api
        var instance;
        var savedTmpApkFilePath = null;

        // private constructor
        function Apktool(_host, _port, _httpProtocol, _webSocketProtocol) {
            this.host = _host;
            this.port = _port;
            this.httpProtocol = _httpProtocol;
            this.webSocketProtocol = _webSocketProtocol;
        }

        function _createInstance() {
            var webSocketProtocol;
            if (window.location.protocol === 'http:')
                webSocketProtocol = 'ws:';
            else if (window.location.protocol === 'https:')
                webSocketProtocol = 'wss:';
            // define a Codemirror mode for smali files
            // TODO make regex respect minimal smali syntax => comment + method declaration
            CodeMirror.defineSimpleMode("modeSmali", {
                // The start state contains the rules that are intially used
                start: [
                    // The regex matches the token, the token property contains the type
                    {regex: /"(?:[^\\]|\\.)*?(?:"|$)/, token: "string"},
                    // You can match multiple tokens at once. Note that the captured
                    // groups must span the whole string in this case
                    {regex: /(method)(\s+)([a-z$][\w$]*)/,
                        token: ["keyword", null, "variable-2"]},
                    // Rules are matched in the order in which they appear, so there is
                    // no ambiguity between this one and the one above
                    {regex: /(\.method|\s?private\s?|\s?protected\s?|\s?public\s?|\s?synthetic\s?|\s?final\s?|\s?static\s?|\s?abstract\s?|\.class|\.super|\.source|\.end.*$|\.locals|\.local|\.prologue|\.line|\.implements|\.field|\.param|.annotation.*|new-instance|return[^\s]+|return\s+|\.catch|const[^\s]+|const\s+|invoke-[^\s]+|move[^\s]+|move\s|if-[^\s]+|do-[^\s]+|iget\s|iget-[^\s]+|iput\s|iput-[^\s]+|sget\s|sget-[^\s]+|sput\s|sput-[^\s]+|aget\s|aget-[^\s]+|aput\s|aput-[^\s]+|new-[^\s]+|array-[^\s]+|fill-[^\s]+|int-to-[^\s]+|double-to-[^\s]+|add-[^\s]+|mul-[^\s]+|rem-[^\s]+|packed-[^\s]+|and-[^\s]+|or-[^\s]+|shl-[^\s]+|ushr-[^\s]+|rsub-[^\s]+|check-cast)\b/,
                        token: "keyword"},
                    {regex: /L.*?;/, token: "atom"},
                    {regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
                        token: "number"},
                    {regex: /#.*/, token: "comment"},
                    //{regex: /(\{(.*?)\}|\s+(v\d+),|\s+(v\d+)\s+|\s+(p\d+),|\s+(p\d+)\s+)/, token: "variable-3"},
                    {regex: /(\{.*?\}|\s?v\d+,|\s?v\d+\s?|\s?p\d+,|\s?p\d+\s?)/, token: "variable-3"},
                    {regex: /->/, token: "operator"},
                    // indent and dedent properties guide autoindentation
                    {regex: /[\{\[\(]/, indent: true},
                    {regex: /[\}\]\)]/, dedent: true},
                    {regex: /[a-z$][\w$]*/, token: "variable"},
                    // You can embed other modes with the mode property. This rule
                    // causes all code between << and >> to be highlighted with the XML
                    // mode.
                    {regex: /<</, token: "meta", mode: {spec: "xml", end: />>/}}
                ],
                // The multi-line comment state.
                comment: [
                    {regex: /.*?\*\//, token: "comment", next: "start"},
                    {regex: /.*/, token: "comment"}
                ],
                // The meta property contains global information about the mode. It
                // can contain properties like lineComment, which are supported by
                // all modes, and also directives like dontIndentStates, which are
                // specific to simple modes.
                meta: {
                    dontIndentStates: ["comment"],
                    lineComment: "#"
                }
            });

            CodeMirror.commands.save = function () {
                notify("AUTO-SAVE mode is enabled by default, you don't need to save changes manually.", "information", 5000);
            };


            return new Apktool(window.location.hostname, window.location.port, window.location.protocol, webSocketProtocol);
        }


        var utils = {
            get_editor_config: function (nodeType, value) {// method that creates an editor configuration depending on its node type (txt, img, audio, video, pdf, others)
                var mode, use_lint;
                switch (nodeType) {
                    case "edit_txt":
                        mode = null;
                        use_lint = false;
                        break;
                    case "edit_smali":
                        mode = 'modeSmali';
                        use_lint = false;
                        break;
                    case "edit_xml":
                        mode = 'xml';
                        use_lint = true;
                        break;
                    case "edit_js":
                    case "edit_json":
                        mode = 'javascript';
                        use_lint = true;
                        break;
                    case "edit_html":
                        mode = 'htmlmixed';
                        use_lint = true;
                        break;
                    case "edit_css":
                        mode = 'css';
                        use_lint = true;
                        break;
                    default:
                        mode = null;
                        use_lint = false;
                        break;
                }

                return {
                    value: value,
                    mode: mode,
                    styleActiveLine: true,
                    highlightSelectionMatches: true,
                    showToken: true,
                    matchBrackets: true,
                    autoCloseBrackets: true,
                    autoCloseTags: true,
                    theme: 'dracula',
                    smartIndent: true,
                    tabSize: 4,
                    tabMode: 'indent',
                    indentWithTabs: true,
                    lineWrapping: false,
                    lineNumbers: true,
                    firstLineNumber: 1,
                    readOnly: false,
                    showCursorWhenSelecting: false,
                    cursorBlinkRate: 0,
                    autofocus: true,
                    viewportMargin: 200,
                    keyMap: 'sublime',
                    extraKeys: {
                        "Ctrl-Space": "autocomplete",
                        "Cmd-Space": "autocomplete",
                        "Alt-F": "findPersistent",
                        "Cmd-F": "findPersistent"
                    },
                    scrollbarStyle: 'native',
                    dragDrop: true,
                    path: 'js/',
                    searchMode: 'inline',
                    foldGutter: true,
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers'],
                    lint: use_lint
                    //scrollbarStyle: 'overlay' // problem scroll bar are not recalculated after resize
                    //scrollbarStyle: 'simple' // problem scroll bar are not recalculated after resize
                };
            }
        };

        // websocket communication wrapper
        var webSocketWrapper = {
            webSocketMessagingProtocol: function (msg) { // wraps the communication protocol between the browser and desktop
                var jsonData = JSON.parse(msg.data);
                if (jsonData.dataType) {
                    if (jsonData.dataType === 'log-event') {
                        if (jsonData.dataLogType) {
                            if (jsonData.dataLogType === 'GENERAL') {
                                // show terminal if not visible
                                if ($('#apkreverse_content').is(':visible')
                                    && !virtualTerminal.jQueryTerminalContainer.is(':visible')
                                    && virtualTerminal.jQueryTerminalContainer.attr('data-size-state') === 'MAXIMIZED') {
                                    virtualTerminal.showVirtualTerminal();
                                }
                                // append log event to virtual terminal
                                virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ' + jsonData.dataMsg,
                                    { raw: true
                                    });
                                // update counter
                                virtualTerminal.update_sidemenu_terminal_counter();
                                virtualTerminal.update_minified_terminal_counter();
                            } else if (jsonData.dataLogType === 'TEXT_SEARCH') {
                                var $div_search_cnt = $('#feBottomTabSearch-content');
                                $div_search_cnt.append('<div>' + jsonData.dataMsg + '</div>');
                                var scrollPosition = $div_search_cnt.height();
                                $('ul#fe-bottom-list-tab-content').scrollTop(scrollPosition);// scroll to bottom => new content always visible
                                $div_search_cnt.attr('data-scroll', scrollPosition);
                            } else if (jsonData.dataLogType === 'EDITOR_LOG') {
                                var $div_log_cnt = $('#feBottomTabLog-content');
                                $div_log_cnt.append('<div>' + jsonData.dataMsg + '</div>');
                                var scrollPositionLog = $div_log_cnt.height();
                                $('ul#fe-bottom-list-tab-content').scrollTop(scrollPositionLog);// scroll to bottom => new content always visible
                                $div_log_cnt.attr('data-scroll', scrollPositionLog);
                            } else if (jsonData.dataLogType === 'DEBUGGER_LOG') {
                                var $div_deb_log_cnt = $('#debugger-lines');
                                // no more than 1000 lines of log => to make filtering smoother
                                if ($("#debugger-lines > div").length > 1000) {
                                    $div_deb_log_cnt.find('div').first().remove();
                                }
                                // check filter and LogLevel
                                var inputInfo = appDebugger.debuggerInputInfo(jsonData.dataMsg);
                                if (inputInfo.canShow) {
                                    $div_deb_log_cnt.append('<div class="' + inputInfo.logClass + '">' + jsonData.dataMsg + '</div>');
                                } else {
                                    $div_deb_log_cnt.append('<div class="' + inputInfo.logClass + '" style="display: none;">' + jsonData.dataMsg + '</div>');
                                }
                                if (appDebugger.alwaysScrollToEnd === true) {
                                    $('#debugger-content-wrapper').scrollTop($div_deb_log_cnt.height());// scroll to bottom => new content always visible
                                }
                            }
                        }
                    } else if (jsonData.dataType === 'process-state') {
                        // update UI depending process type and state
                        var processType = jsonData.dataProcessType;
                        var state = jsonData.dataState;
                        var receivedProcessId = jsonData.dataProcessId;

                        if (state === 'STARTED') {
                            RevEnge.maxMCfg();
                        } else {
                            RevEnge.minMCfg();
                        }


                        if (processType === 'PREVIEW_TEST') {
                            var $btn_cancel_benchmark = $('#cancel-apk-benchmark');
                            var currentProcessId = $btn_cancel_benchmark.attr('data-process-id');
                            if (currentProcessId && receivedProcessId === currentProcessId) {
                                if (state === 'STARTED') {

                                } else if (state === 'COMPLETED') {
                                    $btn_cancel_benchmark.attr('disabled', true);
                                    $btn_cancel_benchmark.attr('data-process-id', '');
                                    $('#benchmark-working-div').hide();
                                    $('#submit-apk-benchmark').hide();
                                    $('form#apkPreviewerDropzone div.dz-preview a.dz-remove').show();
                                } else if (state === 'ERROR') {
                                    $btn_cancel_benchmark.attr('disabled', true);
                                    $btn_cancel_benchmark.attr('data-process-id', '');
                                    $('#benchmark-working-div').hide();
                                    $('#submit-apk-benchmark').show();
                                    $('form#apkPreviewerDropzone div.dz-preview a.dz-remove').show();
                                }
                            }
                        } else if (processType === 'ADB_INSTALL_PREVIEW_APK') {
                            // iterate list of apk ready looking for data-process-id == processId
                            var clickedInstallButton = $("ul#apk-ready-list").find("a[data-process-id='" + receivedProcessId + "']").first();
                            if (state === 'STARTED') {

                            } else if (state === 'COMPLETED' || state === 'ERROR') {
                                // Enable the anchor tag
                                clickedInstallButton.attr('data-process-id', '');
                                clickedInstallButton.removeClass('anchor-disabled').attr("href", "#");
                            }
                        } else if (processType === 'CREATE_NEW_PROJECT') {
                            if (state === 'STARTED') {
                                // freeze UI until project is created
                                $('#apktools-projects-data').block({
                                    message: "Decoding Apk. It may take few minutes, please wait...",
                                    centerY: false,
                                    centerX: false,
                                    css: {
                                        border: 'none',
                                        padding: '15px',
                                        backgroundColor: '#000',
                                        '-webkit-border-radius': '10px',
                                        '-moz-border-radius': '10px',
                                        opacity: .5,
                                        color: '#fff',
                                        position: 'absolute',
                                        margin: 'auto'
                                    } });
                                // show virtual terminal and maximize it
                                virtualTerminal.showVirtualTerminal();
                                virtualTerminal.maximizeVirtualTerminal();
                            } else if (state === 'COMPLETED') {
                                // clean modal
                                Dropzone.options.apkNewProjectDropzone.clean();
                                // unblock user interface modal
                                $('#apktools-projects-data').unblock();
                                // reload projects list
                                ApkToolModule.reloadUiProjectsList();
                                //notify("Your project is ready!", "success", 5000);
                                toastr.success('Your new project is ready!', 'New Project');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("your new project is ready.");
                                }, 700);
                            } else if (state === 'ERROR') {
                                // clean modal
                                Dropzone.options.apkNewProjectDropzone.clean();
                                // unblock user interface modal
                                $.unblockUI();
                                // reload projects list
                                ApkToolModule.reloadUiProjectsList();
                                notify("Project not ready!", "error", 7000);
                            }
                        } else if (processType === 'TEXT_SEARCH') {
                            var $btn_cancel_text_search = $('button#cancel-text-process');
                            $btn_cancel_text_search.attr('disabled', false);

                            if (state === 'STARTED') {
                                $btn_cancel_text_search.attr('data-process-id', receivedProcessId);
                                $btn_cancel_text_search.attr('data-process-type', 'TEXT SEARCH');
                                $btn_cancel_text_search.show();
                            } else if (state === 'COMPLETED' || state === 'ERROR') {
                                $btn_cancel_text_search.attr('data-process-id', '');
                                $btn_cancel_text_search.attr('data-process-type', '');
                                $btn_cancel_text_search.hide();


                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Text search has ended.");
                                }, 700);
                                if (state === 'COMPLETED') {
                                    // ajax get jsTree result
                                    projectEditor.bottomTabsManager.getSearchResult(receivedProcessId);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'TEXT_SEARCH_AND_REPLACE') {
                            if (state === 'STARTED') {

                            } else if (state === 'COMPLETED' || state === 'ERROR') {
                                //toastr.success('Text search has ended!', 'Text search');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Text find and replace has ended.");
                                }, 700);
                                if (state === 'COMPLETED') {
                                    // ajax get jsTree result => update already openeed tabs if their content has changed
                                    projectEditor.bottomTabsManager.getSearchAndReplaceResult(receivedProcessId);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'PACKAGE_NAME_CHANGER') {
                            if (state === 'STARTED') {

                            } else if (state === 'COMPLETED') {
                                toastr.success('Package Name Changer has ended!', 'Package Name Changer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Package name changer has ended with success.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);

                                // copy the project uuid before destroying the editor
                                var projectUuid = $('#project-editor-devenvironment-data').attr('data-project-uuid');
                                // reset editor
                                projectEditor.destroy();
                                // reload project and reset editor
                                projectsListWrapper.open_project_(projectUuid);
                            } else if (state === 'ERROR') {
                                toastr.error('Package Name Changer has ended with error!', 'Package Name Changer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Package name changer has ended with errors.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);
                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'PACKAGE_RENAMER') {
                            if (state === 'STARTED') {

                            } else if (state === 'COMPLETED') {
                                toastr.success('Package Renamer has ended!', 'Package Renamer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Package renamer has ended with success.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);

                                // copy the project uuid before destroying the editor
                                var projectUuid_ = $('#project-editor-devenvironment-data').attr('data-project-uuid');
                                // reset editor
                                projectEditor.destroy();
                                // reload project and reset editor
                                projectsListWrapper.open_project_(projectUuid_);
                            } else if (state === 'ERROR') {
                                toastr.error('Package Renamer has ended with error!', 'Package Renamer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Package renamer has ended with errors.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);
                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'BUILD_DEBUG_APK') {
                            var $btn_cancel_build_debug_apk = $('button#cancel-log-process');
                            $btn_cancel_build_debug_apk.attr('disabled', false);
                            if (state === 'STARTED') {
                                $btn_cancel_build_debug_apk.attr('data-process-id', receivedProcessId);
                                $btn_cancel_build_debug_apk.attr('data-process-type', 'BUILD DEBUG APK');
                                $btn_cancel_build_debug_apk.show();
                            } else if (state === 'COMPLETED') {
                                $btn_cancel_build_debug_apk.attr('data-process-id', '');
                                $btn_cancel_build_debug_apk.attr('data-process-type', '');
                                $btn_cancel_build_debug_apk.hide();

                                toastr.success('Build DEBUG APK has ended with success!', 'Build DEBUG APK');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Debug APK file is ready.");
                                }, 700);

                                projectEditor.bottomTabsManager.reloadApkFilesTab();

                            } else if (state === 'ERROR') {
                                $btn_cancel_build_debug_apk.attr('data-process-id', '');
                                $btn_cancel_build_debug_apk.attr('data-process-type', '');
                                $btn_cancel_build_debug_apk.hide();

                                toastr.error('Build DEBUG APK has ended with error!', 'Build DEBUG APK');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Debug APK file has ended with errors.");
                                }, 700);

                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'BUILD_RELEASE_APK') {
                            var $btn_cancel_build_release_apk = $('button#cancel-log-process');
                            $btn_cancel_build_release_apk.attr('disabled', false);
                            if (state === 'STARTED') {
                                $btn_cancel_build_release_apk.attr('data-process-id', receivedProcessId);
                                $btn_cancel_build_release_apk.attr('data-process-type', 'BUILD RELEASE APK');
                                $btn_cancel_build_release_apk.show();
                            } else if (state === 'COMPLETED') {
                                $btn_cancel_build_release_apk.attr('data-process-id', '');
                                $btn_cancel_build_release_apk.attr('data-process-type', '');
                                $btn_cancel_build_release_apk.hide();

                                toastr.success('Build RELEASE APK has ended with success!', 'Build RELEASE APK');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Release APK file is ready.");
                                }, 700);

                                projectEditor.bottomTabsManager.reloadApkFilesTab();

                            } else if (state === 'ERROR') {
                                $btn_cancel_build_release_apk.attr('data-process-id', '');
                                $btn_cancel_build_release_apk.attr('data-process-type', '');
                                $btn_cancel_build_release_apk.hide();

                                toastr.error('Build RELEASE APK has ended with error!', 'Build RELEASE APK');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Release APK file has ended with errors.");
                                }, 700);

                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'ADB_INSTALL') {
                            var $btn_cancel_adb_install = $('button#cancel-log-process');
                            $btn_cancel_adb_install.attr('disabled', false);
                            if (state === 'STARTED') {
                                $btn_cancel_adb_install.attr('data-process-id', receivedProcessId);
                                $btn_cancel_adb_install.attr('data-process-type', 'APK INSTALL');
                                $btn_cancel_adb_install.show();
                            } else if (state === 'COMPLETED') {
                                $btn_cancel_adb_install.attr('data-process-id', '');
                                $btn_cancel_adb_install.attr('data-process-type', '');
                                $btn_cancel_adb_install.hide();

                                toastr.success('APK INSTALL has ended with success!', 'APK INSTALL');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("APK install, has ended with success.");
                                }, 700);
                            } else if (state === 'ERROR') {
                                $btn_cancel_adb_install.attr('data-process-id', '');
                                $btn_cancel_adb_install.attr('data-process-type', '');
                                $btn_cancel_adb_install.hide();

                                console.log('ADB INSTALL completed ERROR');
                                toastr.error('APK INSTALL has ended with errors!', 'APK INSTALL');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("APK install, has ended with errors.");
                                }, 700);

                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'INSTANT_RUN') {
                            var $btn_cancel_instant_run = $('button#cancel-log-process');
                            $btn_cancel_instant_run.attr('disabled', false);
                            if (state === 'STARTED') {
                                $btn_cancel_instant_run.attr('data-process-id', receivedProcessId);
                                $btn_cancel_instant_run.attr('data-process-type', 'INSTANT RUN');
                                $btn_cancel_instant_run.show();
                            } else if (state === 'COMPLETED') {
                                $btn_cancel_instant_run.attr('data-process-id', '');
                                $btn_cancel_instant_run.attr('data-process-type', '');
                                $btn_cancel_instant_run.hide();

                                toastr.success('INSTANT RUN has ended with success!', 'INSTANT RUN');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("instant run has ended with success.");
                                }, 700);
                            } else if (state === 'ERROR') {
                                $btn_cancel_instant_run.attr('data-process-id', '');
                                $btn_cancel_instant_run.attr('data-process-type', '');
                                $btn_cancel_instant_run.hide();

                                toastr.error('INSTANT RUN has ended with errors!', 'INSTANT RUN');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("instant run has ended with errors.");
                                }, 700);

                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        } else if (processType === 'ADB_INSTALL_SINGLE') { // any apk installer tool
                            var $btn_cancel_adbinstall = $('#cancel-apk-adbinstall');
                            var procId = $btn_cancel_adbinstall.attr('data-process-id');

                            if (procId && receivedProcessId === procId) {
                                if (state === 'STARTED') {

                                } else if (state === 'COMPLETED') {
                                    $btn_cancel_adbinstall.attr('disabled', true);
                                    $btn_cancel_adbinstall.attr('data-process-id', '');
                                    $('#adbinstall-working-div').hide();
                                    $('#submit-apk-adbinstall').hide();
                                    $('form#apkAdbInstallDropzone div.dz-preview a.dz-remove').show();

                                    toastr.success('APK INSTALLER has ended with success!', 'APK INSTALLER');
                                    $.playSound('/static/public/sounds/bell.mp3');
                                    setTimeout(function () {
                                        textToSpeech("APK install has ended with success.");
                                    }, 700);


                                } else if (state === 'ERROR') {
                                    $btn_cancel_adbinstall.attr('disabled', true);
                                    $btn_cancel_adbinstall.attr('data-process-id', '');
                                    $('#adbinstall-working-div').hide();
                                    $('#submit-apk-adbinstall').show();
                                    $('form#apkAdbInstallDropzone div.dz-preview a.dz-remove').show();

                                    toastr.error('APK INSTALLER has ended with errors!', 'APK INSTALLER');
                                    $.playSound('/static/public/sounds/bell.mp3');
                                    setTimeout(function () {
                                        textToSpeech("APK install has ended with errors.");
                                    }, 700);

                                    if (jsonData.dataMessage) {
                                        notify(jsonData.dataMessage, "error", 7000);
                                    }
                                }
                            }
                        } else if (processType === 'MANIFEST_ENTRIES_RENAMER') {
                            if (state === 'STARTED') {

                            } else if (state === 'COMPLETED') {
                                toastr.success('Manifest Entries Transformer has ended!', 'Manifest Entries Transformer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Manifest Entries Transformer has ended with success.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);

                                // copy the project uuid before destroying the editor
                                var projectUuid__ = $('#project-editor-devenvironment-data').attr('data-project-uuid');
                                // reset editor
                                projectEditor.destroy();
                                // reload project and reset editor
                                projectsListWrapper.open_project_(projectUuid__);
                            } else if (state === 'ERROR') {
                                toastr.error('Manifest Entries Transformer has ended with error!', 'Manifest Entries Transformer');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Manifest Entries Transformer has ended with errors.");
                                }, 700);
                                // unblock editor
                                $('#tab-apkreverse-project-editor').unblock();
                                // unblock add style position to relative, unwanted behaviour
                                // thus we must correct it by setting style attribute as empty
                                setTimeout(function () {
                                    $('#tab-apkreverse-project-editor').attr("style", "");
                                }, 700);
                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                            }
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                        }

                    } else if (jsonData.dataType === 'apk-ready-url') {
                        toastr.success('Your apk file is ready!', 'Apk ready');
                        $.playSound('/static/public/sounds/bell.mp3');
                        setTimeout(function () {
                            textToSpeech("A-P-K file is ready.");
                        }, 700);

                        var apkUrl = jsonData.dataUrl;
                        var buildType = jsonData.dataBuildType;
                        var packageName = jsonData.dataPackageName;
                        var iconBase64 = jsonData.dataIconBase64;
                        var sizeInMb = jsonData.size_in_MB;
                        var filePath = jsonData.dataFilePath;

                        var template = $('#apkreverse-apk-ready-info-notification-template').html();
                        var dataTemplate = {
                            apk_ready_package_img_base64: iconBase64,
                            apk_ready_package_name: packageName,
                            apk_ready_build_type: buildType,
                            apk_ready_url: apkUrl,
                            apk_ready_size: sizeInMb,
                            apk_ready_file_path: filePath
                        };
                        var html = Mustache.to_html(template, dataTemplate);
                        $("ul#apk-ready-list").prepend(html).fadeIn();
                    } else if (jsonData.dataType === 'user-projects-total-size') {
                        // populate total size span
                        var totalSizeInBytes = jsonData.dataSize;
                        $("span#user-projects-total-size-txt").html(formatBytes(totalSizeInBytes));
                        // hide loader image
                        $("img#user-projects-total-size-loader").hide();
                    }
                }
            }
        };

        // virtual terminal wrapper
        var virtualTerminal = {
            sidemenu_terminal_log_counter: 0,
            minified_terminal_log_counter: 0,
            jQueryTerminal: null,
            jQueryTerminalContainer: null,
            initJQueryTerminal: function () {
                // init virtual terminal
                this.jQueryTerminal = $('#jquery-terminal').terminal({
                    prompt: 'revenge-desktop> ',
                    greetings: "Welcome to RevEnge terminal"
                });
                // init resizable container
                this.jQueryTerminalContainer = $('#jquery-terminal-resizable');
                this.jQueryTerminalContainer.resizable({
                    handles: {
                        'nw': '#nwgrip',
                        'n': '#ngrip',
                        'w': '#wgrip'
                    }
                });
            },
            hideAndResetSideMenuEventCounter: function () {
                var $sideMenuEventCounter = $('#sidemenu-terminal-counter');
                $sideMenuEventCounter.text('');
                $sideMenuEventCounter.hide();
                virtualTerminal.sidemenu_terminal_log_counter = 0;
            },
            update_sidemenu_terminal_counter: function () {
                var $sideMenuEventCounter = $('#sidemenu-terminal-counter');
                if (!$('#apkreverse_content').is(':visible')) {
                    if (!$sideMenuEventCounter.is(":visible")) {
                        $sideMenuEventCounter.show();
                    }
                    virtualTerminal.sidemenu_terminal_log_counter += 1;
                    $sideMenuEventCounter.text(this.sidemenu_terminal_log_counter);
                } else {
                    if (virtualTerminal.sidemenu_terminal_log_counter !== 0) {
                        virtualTerminal.sidemenu_terminal_log_counter = 0;
                        $sideMenuEventCounter.text('');
                    }
                    if ($sideMenuEventCounter.is(':visible')) {
                        $sideMenuEventCounter.hide();
                    }
                }
            },
            update_minified_terminal_counter: function () {
                var $minifiedEventCounter = $('#minified-terminal-counter');
                if ($('#jquery-terminal-resizable').attr('data-size-state') === 'MINIMIZED') {
                    if (!$minifiedEventCounter.is(":visible")) {
                        $minifiedEventCounter.show();
                    }
                    virtualTerminal.minified_terminal_log_counter += 1;
                    $minifiedEventCounter.text(virtualTerminal.minified_terminal_log_counter);
                }
            },
            showVirtualTerminal: function () {
                if (virtualTerminal.jQueryTerminalContainer && !virtualTerminal.jQueryTerminalContainer.is(":visible")) {
                    virtualTerminal.jQueryTerminalContainer.fadeIn({queue: false, duration: 'slow'}).animate({ opacity: 1 }, 'slow');
                }
            },
            minimizeVirtualTerminal: function (e) {
                e.preventDefault();
                if (this.jQueryTerminalContainer) {
                    virtualTerminal.minified_terminal_log_counter = 0;
                    var $minifiedEventCounter = $('#minified-terminal-counter');
                    $minifiedEventCounter.hide('');
                    $minifiedEventCounter.text('');
                    virtualTerminal.jQueryTerminalContainer.fadeOut({queue: false, duration: 1000}).animate({ width: 200, height: 110 }, 1000, function () {
                        // on animation complete, show minified-terminal
                        $('#minified-terminal').fadeIn({queue: false, duration: 'fast'});
                        $('#jquery-terminal-resizable').attr('data-size-state', 'MINIMIZED');
                    });
                }
            },
            maximizeVirtualTerminal: function (e) {
                if (e) {
                    e.preventDefault();
                }
                if (this.jQueryTerminalContainer) {
                    virtualTerminal.minified_terminal_log_counter = 0;
                    $('#minified-terminal').fadeOut({queue: false, duration: 'fast'}).animate({}, 'fast', function () {
                        virtualTerminal.minified_terminal_log_counter = 0;
                        virtualTerminal.jQueryTerminalContainer.fadeIn({queue: false, duration: 1000}).animate({ width: 640, height: 320 }, 1000);
                        $('#jquery-terminal-resizable').attr('data-size-state', 'MAXIMIZED');
                    });
                }
            }
        };

        // banchmark apk wrapper
        var benchmark = {
            benchmarkApk: function () { // test if an apk can be reverse engineered
                console.log("Benchmarking apk: " + savedTmpApkFilePath);
                $.ajax({
                    url: "/api/protected/benchmarkTmpApkFileHandler",
                    data: {savedTmpApkFilePath: savedTmpApkFilePath},
                    type: 'POST',
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    success: function (data) {
                        $('#submit-apk-benchmark').hide();
                        $('#benchmark-working-div').show();
                        $('form#apkPreviewerDropzone div.dz-preview a.dz-remove').hide();

                        var $btn_cancel_benchmark = $('#cancel-apk-benchmark');
                        $btn_cancel_benchmark.attr('data-process-id', data.processId);
                        $btn_cancel_benchmark.attr('disabled', false);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    }
                });
            },
            cancelBenchmarkApk: function () { // cancel current running benchmark
                RevEnge.minMCfg();
                var $btn_cancel_benchmark = $('#cancel-apk-benchmark');
                $btn_cancel_benchmark.attr('disabled', true);
                console.log("Benchmarking apk: " + savedTmpApkFilePath);
                $.ajax({
                    url: "/api/protected/cancelAplToolProcessHandler",
                    data: {processId: $btn_cancel_benchmark.attr('data-process-id')},
                    type: 'POST',
                    timeout: 180000,
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    success: function (data) {
                        $('#benchmark-working-div').hide();
                        $btn_cancel_benchmark.attr('data-process-id', '');

                        $('#submit-apk-benchmark').show();
                        $('form#apkPreviewerDropzone div.dz-preview a.dz-remove').show();

                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ************************',
                            { raw: true
                            });

                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> *** Process canceled ***',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ************************',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ' + data.message,
                            { raw: true
                            });
                        // update counter
                        virtualTerminal.update_sidemenu_terminal_counter();
                        virtualTerminal.update_minified_terminal_counter();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        $btn_cancel_benchmark.attr('disabled', false);
                    }
                });
            }
        };


        // adb install apk wrapper
        var adbInstall = {
            adbInstallkApk: function () {
                $.ajax({
                    url: "/api/protected/adbinstallTmpApkFileHandler",
                    data: {action: "ADB_INSTALL", savedTmpApkFilePath: encodeURIComponent(Dropzone.options.apkAdbInstallDropzone.savedTmpFilePath)},
                    type: 'POST',
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    success: function (data) {
                        $('#submit-apk-adbinstall').hide();
                        $('#adbinstall-working-div').show();
                        $('form#apkAdbInstallDropzone div.dz-preview a.dz-remove').hide();

                        var $btn_cancel_adbinstall = $('#cancel-apk-adbinstall');
                        $btn_cancel_adbinstall.attr('data-process-id', data);
                        $btn_cancel_adbinstall.attr('disabled', false);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 5000);
                    }
                });
            },
            cancelAdbInstallApk: function () { // cancel current running benchmark
                RevEnge.minMCfg();
                var $btn_cancel_adbinstall = $('#cancel-apk-adbinstall');
                $btn_cancel_adbinstall.attr('disabled', true);

                $.ajax({
                    url: "/api/protected/adbinstallTmpApkFileHandler",
                    data: {action: "CANCEL_ADB_INSTALL", processId: $btn_cancel_adbinstall.attr('data-process-id')},
                    type: 'POST',
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    success: function (data) {
                        $('#adbinstall-working-div').hide();
                        $btn_cancel_adbinstall.attr('data-process-id', '');

                        $('#submit-apk-adbinstall').show();
                        $('form#apkAdbInstallDropzone div.dz-preview a.dz-remove').show();

                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ************************',
                            { raw: true
                            });

                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> *** Process canceled ***',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ************************',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ',
                            { raw: true
                            });
                        virtualTerminal.jQueryTerminal.echo('srvc@' + instance.host + '> ' + data,
                            { raw: true
                            });
                        // update counter
                        virtualTerminal.update_sidemenu_terminal_counter();
                        virtualTerminal.update_minified_terminal_counter();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        $btn_cancel_adbinstall.attr('disabled', false);
                    }
                });
            }
        };

        // jsTree file wrapper
        var jsTreeWrapper = {
            filterJsTreeApkPreviewDebounce: debounce(function (input, e) { // Js Tree filter search debounce ==> delay filtering
                var code = e.keyCode || e.which;
                // tab=9, shift=16, ctrl=17 , arrows =37..40
                if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                if (code == '27') $(input).val('');
                // same word in all filters inputs
                //$('#js-tree-apk-preview-list-count').val($(input).val());
                jsTreeWrapper.filterJsTreeApkPreviewSearch($(input).val());
            }, 250),
            filterJsTreeApkPreviewSearch: function (textFilter) { // Js Tree filter search
                var searchResult = $('#jstree-tree-apk-prev').jstree('search', textFilter);
                var searchCount = $(searchResult).find('.jstree-search').length;
                var spanFilter = $('span#filter-counter-js-tree-apk-prev');
                if (searchCount > 0) {
                    spanFilter.text(searchCount.toString());
                    spanFilter.show();
                } else {
                    spanFilter.text('0');
                    spanFilter.hide();
                }
            },
            // be careful, do not use this function for large trees, it will hang the browser, because it is recursive
            convertToHierarchy: function (fileNamesList) { // build tree Hierarchy form list of file names
                var arry = [];
                for (var k = 0; k < fileNamesList.length; k++) {
                    arry.push(fileNamesList[k].split("/"))
                }
                //var item, path;

                // Discard duplicates and set up parent/child relationships
                var children = {};
                var hasParent = {};
                for (var i = 0; i < arry.length; i++) {
                    var path = arry[i];
                    var parent = null;
                    for (var j = 0; j < path.length; j++) {
                        var item_ = path[j];
                        if (!children[item_]) {
                            children[item_] = {};
                        }
                        if (parent) {
                            children[parent][item_] = true;
                            /* dummy value */
                            hasParent[item_] = true;
                        }
                        parent = item_;
                    }
                }

                // Now build the hierarchy
                var result = [];
                for (var item in children) {
                    if (!hasParent[item]) {
                        result.push(jsTreeWrapper.buildNodeRecursive(item, children));
                    }
                }
                //console.log("Hierarchy ==========> "+ JSON.stringify(result));
                return result;
            },
            buildNodeRecursive: function (item, children) {
                var id = Math.random() * 10000000000;
                var node = {id: id, type: 'file', text: item, children: []};
                for (var child in children[item]) {
                    if (node.type === 'file') {
                        node.type = 'folder';
                    }
                    node.children.push(jsTreeWrapper.buildNodeRecursive(child, children));
                }
                if (node.type === 'folder') {
                    node.text = node.text + ' (' + node.children.length + ')';
                }
                return node;
            }
        };

        // UI wrapper
        var main_UI_loader = {
            openCreateProjectModal: function (e) {
                e.preventDefault();
                // clean modal
                Dropzone.options.apkNewProjectDropzone.clean();
                // show modal
                $('#modalCreateProject').modal('show');
            },
            initUi: function () {
                // editor tabs context menu
                $.contextMenu({
                    selector: 'div#tabs_file_editor div table tr td',
                    callback: function (key, options) {
                        var tabId = $(this).attr("id").replace("tabs_tabs_file_editor_tab_", "");
                        ApkToolModule.editorTabsContextMenuClicked(tabId, key);
                    },
                    items: {
                        "close": {name: "Close"},
                        "close_others": {name: "Close others"},
                        "close_all": {name: "Close all"}
                    }
                });

                // Resize fileEditor layout and Debugger layout when their tab is shown
                // if not done the file manager UI component (w2ui) wont stretch to fit the width of the parent
                $('body').on('shown.bs.tab', 'a[href="#tab-apkreverse-project-editor"]', function () {
                    // Resize fileEditor
                    try {
                        w2ui['layout_file_editor'].resize();
                    } catch (err) {
                        // do nothing
                    }
                }).on('shown.bs.tab', 'a[href="#apktools-projects-data"]', function () {
                    if ($('div#tab-apkreverse-project-editor').length != 0) {
                        // Resize fileEditor
                        try {
                            w2ui['layout_file_editor'].resize();
                        } catch (err) {
                            // do nothing
                        }
                    }
                }).on('shown.bs.tab', 'a[href="#apktools-debugger-data"]', function () {
                    // Resize debugger
                    try {
                        w2ui['layout_debugger'].resize();
                    } catch (err) {
                        // do nothing
                    }
                });

                // max length plugin
                $('.max-length').maxlength({
                    alwaysShow: true,
                    appendToParent: true
                });

                // init debugger
                appDebugger.init();


                // global dropzone variable for apk previewer
                Dropzone.options.apkPreviewerDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 500,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    acceptedFiles: ".apk",
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    init: function () {
                        apkPreviewerDropzone = this;

                        var benchmarkButton = $("#submit-apk-benchmark");
                        /*benchmarkButton.addEventListener("click", function () {
                         });*/
                        benchmarkButton.hide();


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                            // Show submit button here and/or inform user to click it.
                            //var files = apkPreviewerDropzone.getAcceptedFiles();
                            // Tell Dropzone to process all queued files.
                            //apkPreviewerDropzone.processFile(file);
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone processing');
                            this.options.url = "/api/protected/uploadApkPreviewerHandler";
                        });

                        this.on('success', function (file, response) {
                            console.log('Dropzone success');
                            savedTmpApkFilePath = decodeURIComponent(response.savedTmpApkFilePath);

                            if (response.ic_launcher_bytes_resized) {
                                apkPreviewerDropzone.emit("thumbnail", file, "data:image/png;base64," + response.ic_launcher_bytes_resized);
                            }

                            var template = $('#apkreverse-apk-previewer-result-template').html();
                            var dataTemplate = {
                                response: response,
                                apk_file_name: file.name
                            };
                            var html = Mustache.to_html(template, dataTemplate);
                            $("div#apkreverse-apk-previewer-result-container").html(html);

                            // js-tree init
                            $('#jstree-tree-apk-prev')
                                .jstree({
                                    core: {
                                        themes: {
                                            name: "default",
                                            dots: true,
                                            icons: true,
                                            url: "/static/public/plugins/jstree/themes/default/style.min.css"
                                        },
                                        data: jsTreeWrapper.convertToHierarchy(response.assets)
                                    },
                                    plugins: ['themes', 'html_data', 'types', 'wholerow', 'search', 'sort'],
                                    types: {
                                        folder: {
                                            icon: '/static/public/plugins/jstree/b_folder.png'
                                        },

                                        file: {
                                            icon: '/static/public/plugins/jstree/a_file.png'
                                        }
                                    },
                                    sort: function (a, b) { // sort by icon and after by text
                                        var a1 = this.get_node(a);
                                        var b1 = this.get_node(b);
                                        if (a1.icon == b1.icon) {
                                            return (a1.text > b1.text) ? 1 : -1;
                                        } else {
                                            return (a1.icon > b1.icon) ? 1 : -1;
                                        }
                                    },
                                    search: {
                                        case_insensitive: true,
                                        show_only_matches: true
                                    }
                                });
                            // js-tree search plugin init
                            $("#js-search-apk-prev-input").keyup(function (e) {
                                jsTreeWrapper.filterJsTreeApkPreviewDebounce(this, e);
                            });
                            benchmarkButton.show();
                            console.log("done apk previewer");
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 7000);
                            if (!file.accepted)
                                this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone thumbnail');
                            console.log("Added thumbnail...");
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            // reset UI
                            $("div#apkreverse-apk-previewer-result-container").html('');
                            benchmarkButton.hide();
                            console.log("deleting savedTmpApkFilePath = " + savedTmpApkFilePath);
                            // ajax delete tmp file from katna-desktop
                            if (!savedTmpApkFilePath)
                                return;
                            $.ajax({
                                url: "/api/protected/deleteTmpFileHandler" + "?savedTmpFilePath=" + encodeURIComponent(savedTmpApkFilePath),
                                type: 'DELETE',
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function () {
                                    savedTmpApkFilePath = null;
                                    console.log("delete apk success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#apkPreviewerDropzone").dropzone(Dropzone.options.apkPreviewerDropzone);


                // global dropzone variable for apk installer
                Dropzone.options.apkAdbInstallDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 500,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    acceptedFiles: ".apk",
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    init: function () {
                        apkAdbInstallDropzone = this;

                        var installButton = $("#submit-apk-adbinstall");
                        installButton.hide();


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                            // Show submit button here and/or inform user to click it.
                            //var files = apkPreviewerDropzone.getAcceptedFiles();
                            // Tell Dropzone to process all queued files.
                            //apkPreviewerDropzone.processFile(file);
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone processing');
                            this.options.url = "/api/protected/uploadApkPreviewerHandler";
                        });

                        this.on('success', function (file, response) {
                            console.log('Dropzone success');
                            Dropzone.options.apkAdbInstallDropzone.savedTmpFilePath = decodeURIComponent(response.savedTmpApkFilePath);

                            if (response.ic_launcher_bytes_resized) {
                                apkAdbInstallDropzone.emit("thumbnail", file, "data:image/png;base64," + response.ic_launcher_bytes_resized);
                            }

                            var template = $('#apkreverse-apk-adbinstall-result-template').html();
                            var dataTemplate = {
                                response: response,
                                apk_file_name: file.name
                            };
                            var html = Mustache.to_html(template, dataTemplate);
                            $("div#apkreverse-apk-adbinstall-result-container").html(html);

                            // js-tree init
                            $('#jstree-tree-apk-adbinstall')
                                .jstree({
                                    core: {
                                        themes: {
                                            name: "default",
                                            dots: true,
                                            icons: true,
                                            url: "/static/public/plugins/jstree/themes/default/style.min.css"
                                        },
                                        data: jsTreeWrapper.convertToHierarchy(response.assets)
                                    },
                                    plugins: ['themes', 'html_data', 'types', 'wholerow', 'search', 'sort'],
                                    types: {
                                        folder: {
                                            icon: '/static/public/plugins/jstree/b_folder.png'
                                        },

                                        file: {
                                            icon: '/static/public/plugins/jstree/a_file.png'
                                        }
                                    },
                                    sort: function (a, b) { // sort by icon and after by text
                                        var a1 = this.get_node(a);
                                        var b1 = this.get_node(b);
                                        if (a1.icon == b1.icon) {
                                            return (a1.text > b1.text) ? 1 : -1;
                                        } else {
                                            return (a1.icon > b1.icon) ? 1 : -1;
                                        }
                                    },
                                    search: {
                                        case_insensitive: true,
                                        show_only_matches: true
                                    }
                                });


                            var filterJsTreeApkInfoSearch = function (textFilter) { // Js Tree filter search
                                var searchResult = $('#jstree-tree-apk-adbinstall').jstree('search', textFilter);
                                var searchCount = $(searchResult).find('.jstree-search').length;
                                var spanFilter = $('span#filter-counter-js-tree-apk-adbinstall');
                                if (searchCount > 0) {
                                    spanFilter.text(searchCount.toString());
                                    spanFilter.show();
                                } else {
                                    spanFilter.text('0');
                                    spanFilter.hide();
                                }
                            };

                            var filterJsTreeApkInfoDebounce = debounce(function (input, e) { // Js Tree filter search debounce ==> delay filtering
                                var code = e.keyCode || e.which;
                                // tab=9, shift=16, ctrl=17 , arrows =37..40
                                if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                                if (code == '27') $(input).val('');
                                // same word in all filters inputs
                                filterJsTreeApkInfoSearch($(input).val());
                            }, 250);
                            // js-tree search plugin init
                            $("#js-search-apk-adbinstall-input").keyup(function (e) {
                                filterJsTreeApkInfoDebounce(this, e);
                            });
                            installButton.show();
                            console.log("done apk adbinstall preview");
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 7000);
                            if (!file.accepted)
                                this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone thumbnail');
                            console.log("Added thumbnail...");
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            // reset UI
                            $("div#apkreverse-apk-adbinstall-result-container").html('');
                            installButton.hide();
                            // ajax delete tmp file
                            if (!Dropzone.options.apkAdbInstallDropzone.savedTmpFilePath)
                                return;
                            $.ajax({
                                url: "/api/protected/deleteTmpFileHandler" + "?savedTmpFilePath=" + encodeURIComponent(Dropzone.options.apkAdbInstallDropzone.savedTmpFilePath),
                                type: 'DELETE',
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function () {
                                    Dropzone.options.apkAdbInstallDropzone.savedTmpFilePath = null;
                                    console.log("delete apk success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#apkAdbInstallDropzone").dropzone(Dropzone.options.apkAdbInstallDropzone);

                // global dropzone variable for apk new project modal
                Dropzone.options.apkNewProjectDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 500,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    acceptedFiles: ".apk",
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    clean: function () {
                        Dropzone.forElement("#apkNewProjectDropzone").removeAllFiles(true);
                        // reset UI
                        $("div#new-project-apkinfo-content").html('');
                        $("#new-project-next-step").hide();

                        // clean sub-modal
                        $('input#new_project_name_input').val('');
                        $('#new-project-name-submodal').submodal('hide');
                    },

                    init: function () {
                        apkNewProjectDropzone = this;

                        var createNewProjectNextStepButton = $("#new-project-next-step");
                        /*benchmarkButton.addEventListener("click", function () {
                         });*/
                        createNewProjectNextStepButton.hide();


                        this.on('maxfilesexceeded', function (file) {
                            console.log('max files exceeded => removing file!');
                        });


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone processing');
                            this.options.url = "/api/protected/uploadTmpFile";
                        });

                        this.on('success', function (file, response) {
                            console.log('Dropzone success');
                            Dropzone.options.apkNewProjectDropzone.savedTmpFilePath = decodeURIComponent(response);
                            console.log("savedTmpFilePath = " + Dropzone.options.apkNewProjectDropzone.savedTmpFilePath);

                            // ajax get Apk info
                            $.ajax({
                                url: "/api/protected/getApkInfo",
                                data: {filePath: encodeURIComponent(Dropzone.options.apkNewProjectDropzone.savedTmpFilePath)},
                                method: 'GET',
                                dataType: "json",
                                timeout: 15000,
                                cache: false,
                                success: function (response) {
                                    // show create project button + populate apk info template
                                    createNewProjectNextStepButton.show();
                                    Dropzone.options.apkNewProjectDropzone.currentFileName = file.name;
                                    // update thumbnail
                                    if (response.ic_launcher_bytes_resized) {
                                        apkNewProjectDropzone.emit("thumbnail", file, "data:image/png;base64," + response.ic_launcher_bytes_resized);
                                    }
                                    var template = $('#new-project-apkinfo-content-template').html();
                                    var dataTemplate = {
                                        response: response,
                                        apk_file_name: file.name
                                    };
                                    var html = Mustache.to_html(template, dataTemplate);
                                    $("div#new-project-apkinfo-content").html(html);
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 7000);
                            if (!file.accepted)
                                this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone thumbnail');
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            // reset UI
                            $("div#new-project-apkinfo-content").html('');
                            createNewProjectNextStepButton.hide();
                            // ajax delete tmp file
                            if (!Dropzone.options.apkNewProjectDropzone.savedTmpFilePath)
                                return;

                            $.ajax({
                                url: "/api/protected/deleteTmpFileHandler" + "?savedTmpFilePath=" + encodeURIComponent(Dropzone.options.apkNewProjectDropzone.savedTmpFilePath),
                                type: 'DELETE',
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function () {
                                    Dropzone.options.apkNewProjectDropzone.savedTmpFilePath = null;
                                    console.log("delete file success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#apkNewProjectDropzone").dropzone(Dropzone.options.apkNewProjectDropzone);


                // global dropzone variable for editor context menu item : "add files" to a project folder
                Dropzone.options.addFilesToProjectFolderDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 500,
                    uploadMultiple: true,
                    parallelUploads: 1,
                    maxFiles: 500,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    init: function () {
                        addFilesToProjectFolderDropzone = this;

                        // Setup the event listener for buttons
                        var removeAllFilesButton = document.querySelector("button#clear-add-files-dropzone");
                        removeAllFilesButton.addEventListener('click', function (event) {
                            // cancel uploads as well, call removeAllFiles(true);
                            addFilesToProjectFolderDropzone.removeAllFiles(true);
                        });

                        // modal submit button
                        var submitButton = document.querySelector("button#modalAddFilesToProjectFolder-submit");
                        submitButton.addEventListener('click', function (event) {
                            if (!addFilesToProjectFolderDropzone.getAcceptedFiles().length) {
                                notify('You must add at least one file before submitting!', 'warning', 5000);
                            } else if (addFilesToProjectFolderDropzone.getUploadingFiles().length) {
                                notify('Please wait for files to upload', 'warning', 5000);
                            } else {
                                $.ajax({
                                    url: "/api/protected/contextMenuForEditor",
                                    type: 'POST',
                                    data: {action: "ADD_FILES", subAction: "SUBMIT", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                        tmpFolderName: $('#modalAddFilesToProjectFolder-temp-folder-name').val(), nodeId: $('#modalAddFilesToProjectFolder-target-nodeId').val()},
                                    xhrFields: {
                                        withCredentials: true
                                    },
                                    crossDomain: true,
                                    timeout: 180000,
                                    beforeSend: function () {
                                        showBusysign();
                                    },
                                    success: function (data) {
                                        console.log("add files : " + data.message);
                                        if (data.message === "COPY_STRATEGY") {
                                            // // populate submodal duplication counter
                                            $('#add-new-files-duplicates-count').html(data.intersection.length + ' file');
                                            // populate submodal duplication list
                                            var duplicates = "";
                                            for (var i = 0; i < data.intersection.length; i++) {
                                                duplicates += data.intersection[i] + '<br>'
                                            }
                                            $('#add-new-files-duplicates-list').html(duplicates);
                                            // open sub modal
                                            $('#add-new-files-duplicates-submodal').submodal('show')
                                        } else if (data.message === "COPY_SUCCESS") {
                                            var tree = $("#jstree-tree-fileeditor");
                                            var parentNodeId = $('#modalAddFilesToProjectFolder-target-nodeId').val();
                                            var parentNode = tree.jstree().get_node(parentNodeId);
                                            var newNodesIdsArray = []; // an array that will contain the IDs of the newly created nodes

                                            // add nodes to jsTree only if parent is loaded
                                            if (parentNode.state.loaded === true) {
                                                // add nodes to jsTree and check if node is opened, if opened refresh it with the new content
                                                for (var k = 0; k < data.new_nodes.length; k++) {
                                                    newNodesIdsArray.push(data.new_nodes[k].id);
                                                    tree.jstree(true).create_node(parentNodeId, data.new_nodes[k], "first", function () {
                                                    }, true);
                                                }
                                                tree.jstree("deselect_all");
                                                tree.jstree('select_node', newNodesIdsArray);
                                            }
                                            // update parent size if parent is not the root folder
                                            if (parentNodeId != '#' && parentNodeId != '0') {
                                                var parentName = parentNode.text;
                                                var sizeArray = parentName.match(/\([\d]+\)/g).map(function (val) {
                                                    return val.replace(/\(|\)/g, '');
                                                });

                                                if (sizeArray.length == 1) {
                                                    try {
                                                        var newSize = parseInt(sizeArray[0]) + data.new_nodes.length;
                                                        var parentNewName = parentName.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize + ')';
                                                        tree.jstree(true).rename_node(parentNodeId, parentNewName);
                                                    } catch (err) {
                                                        // do nothing
                                                        console.log(err.toString());
                                                    }
                                                }
                                            }

                                            // close the modal
                                            $('#modalAddFilesToProjectFolder').modal('hide');
                                        }
                                    },
                                    error: function (xhr) {
                                        var msg;
                                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                                            msg = xhr.statusText;
                                        } else {
                                            msg = xhr.statusText + ": " + xhr.responseText;
                                        }
                                        notify(msg, "error", 7000);
                                    },
                                    complete: function () {
                                        hideBusysign();
                                    }
                                });
                            }
                        });

                        // submodal override button
                        var submodalOverrideButton = document.querySelector("button#modalAddFilesToProjectFolder-submodal-override");
                        submodalOverrideButton.addEventListener('click', function (event) {
                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                type: 'POST',
                                data: {action: "ADD_FILES", subAction: "SUBMIT", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                    tmpFolderName: $('#modalAddFilesToProjectFolder-temp-folder-name').val(),
                                    nodeId: $('#modalAddFilesToProjectFolder-target-nodeId').val(), copyStrategy: "OVERRIDE"},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                timeout: 180000,
                                beforeSend: function () {
                                    showBusysign();
                                },
                                success: function (data) {
                                    if (data.message === "COPY_SUCCESS") {

                                        var tree = $("#jstree-tree-fileeditor");
                                        var parentNodeId = $('#modalAddFilesToProjectFolder-target-nodeId').val();
                                        var parentNode = tree.jstree().get_node(parentNodeId);
                                        var newNodesIdsArray = []; // an array that will contain the IDs of the newly created nodes
                                        var updatedNodesIdsArray = []; // an array that will contain the IDs of updated nodes

                                        // add nodes to jsTree only if parent is loaded
                                        if (parentNode.state.loaded === true) {
                                            // add nodes to jsTree and check if node is opened, if opened refresh it with the new content
                                            for (var k = 0; k < data.new_nodes.length; k++) {
                                                newNodesIdsArray.push(data.new_nodes[k].id);
                                                tree.jstree(true).create_node(parentNodeId, data.new_nodes[k], "first", function () {
                                                }, true);
                                            }

                                            // do not create updated nodes as they already exists
                                            for (var l = 0; l < data.updated_nodes.length; l++) {
                                                updatedNodesIdsArray.push(data.updated_nodes[l].id);
                                            }

                                            tree.jstree("deselect_all");
                                            tree.jstree('select_node', newNodesIdsArray.concat(updatedNodesIdsArray));
                                        }
                                        // update parent size if parent is not the root folder
                                        if (parentNodeId != '#' && parentNodeId != '0') {
                                            var parentName = parentNode.text;
                                            var sizeArray = parentName.match(/\([\d]+\)/g).map(function (val) {
                                                return val.replace(/\(|\)/g, '');
                                            });

                                            if (sizeArray.length == 1) {
                                                try {
                                                    var newSize = parseInt(sizeArray[0]) + data.new_nodes.length;
                                                    var parentNewName = parentName.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize + ')';
                                                    tree.jstree(true).rename_node(parentNodeId, parentNewName);
                                                } catch (err) {
                                                    // do nothing
                                                    console.log(err.toString());
                                                }
                                            }
                                        }
                                        // check opened tabs if contains an updated node, if so => update its content
                                        if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                            for (var p = 0; p < w2ui['tabs_file_editor'].tabs.length; p++) {
                                                var tmpNodeId = w2ui['tabs_file_editor'].tabs[p].id;
                                                var tmpNode = tree.jstree().get_node(tmpNodeId);

                                                if (updatedNodesIdsArray.indexOf(String(tmpNodeId)) > -1) { // tab is opened, its content has been overridden => must refresh the tab content
                                                    console.log("updating tab : " + tmpNode.text);
                                                    // get file content from server
                                                    $.ajax({
                                                        url: "/api/protected/getFileContent",
                                                        data: {nodeId: tmpNodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                                        method: 'GET',
                                                        dataType: "json",
                                                        timeout: 30000,
                                                        cache: false,
                                                        success: function (data) {
                                                            projectEditor.fileWrapper.updateTabContent(data);
                                                        },
                                                        error: function (xhr) {
                                                            var msg;
                                                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                                msg = xhr.statusText;
                                                            } else {
                                                                msg = xhr.statusText + ": " + xhr.responseText;
                                                            }
                                                            notify(msg, "error", 7000);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                        // close the modal
                                        $('#modalAddFilesToProjectFolder').modal('hide');
                                    }
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                complete: function () {
                                    hideBusysign();
                                }
                            });
                        });

                        // submodal rename button
                        var submodalRenameButton = document.querySelector("button#modalAddFilesToProjectFolder-submodal-rename");
                        submodalRenameButton.addEventListener('click', function (event) {
                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                type: 'POST',
                                data: {action: "ADD_FILES", subAction: "SUBMIT", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                    tmpFolderName: $('#modalAddFilesToProjectFolder-temp-folder-name').val(),
                                    nodeId: $('#modalAddFilesToProjectFolder-target-nodeId').val(), copyStrategy: "RENAME"},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                timeout: 180000,
                                beforeSend: function () {
                                    showBusysign();
                                },
                                success: function (data) {
                                    if (data.message === "COPY_SUCCESS") {
                                        var tree = $("#jstree-tree-fileeditor");
                                        var parentNodeId = $('#modalAddFilesToProjectFolder-target-nodeId').val();
                                        var parentNode = tree.jstree().get_node(parentNodeId);
                                        var newNodesIdsArray = []; // an array that will contain the IDs of the newly created nodes

                                        // add nodes to jsTree only if parent is loaded, and select them
                                        if (parentNode.state.loaded === true) {
                                            // add nodes to jsTree and check if node is opened, if opened refresh it with the new content
                                            for (var k = 0; k < data.new_nodes.length; k++) {
                                                newNodesIdsArray.push(data.new_nodes[k].id);
                                                tree.jstree(true).create_node(parentNodeId, data.new_nodes[k], "first", function () {
                                                }, true);
                                            }
                                            tree.jstree("deselect_all");
                                            tree.jstree('select_node', newNodesIdsArray);
                                        }
                                        // update parent size if parent is not root folder
                                        if (parentNodeId != '#' && parentNodeId != '0') {
                                            var parentName = parentNode.text;
                                            var sizeArray = parentName.match(/\([\d]+\)/g).map(function (val) {
                                                return val.replace(/\(|\)/g, '');
                                            });

                                            if (sizeArray.length == 1) {
                                                try {
                                                    var newSize = parseInt(sizeArray[0]) + data.new_nodes.length;
                                                    var parentNewName = parentName.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize + ')';
                                                    tree.jstree(true).rename_node(parentNodeId, parentNewName);
                                                } catch (err) {
                                                    // do nothing
                                                    console.log(err.toString());
                                                }
                                            }
                                        }

                                        // close the modal
                                        $('#modalAddFilesToProjectFolder').modal('hide');
                                    }
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                complete: function () {
                                    hideBusysign();
                                }
                            });
                        });


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone "add files" on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone "add files" > added file');
                            // Tell Dropzone to process all queued files.
                            //apkPreviewerDropzone.processFile(file);

                            // Show/hide buttons here
                            if (addFilesToProjectFolderDropzone.files.length) {
                                removeAllFilesButton.style.display = 'block';
                                submitButton.style.display = 'inline-block';
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                submitButton.style.display = 'none';
                            }
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone "add files" processing');
                            this.options.url = "/api/protected/contextMenuForEditor?action=ADD_FILES&subAction=UPLOAD_FILE&projectUuid=" +
                                $('#project-editor-devenvironment-data').attr('data-project-uuid') + "&tmpFolderName=" + $('#modalAddFilesToProjectFolder-temp-folder-name').val() + "&fileName=" + btoa(file.name);
                        });

                        this.on("sending", function (file, xhr, formData) {
                            console.log('Sending the file' + file.name);
                            formData.append('uploaded_file', file);
                        });

                        this.on('success', function (file, response) {
                            console.log("add files done with success");
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 5000);
                            //if (!file.accepted)
                            this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone "add files " thumbnail');
                        });

                        this.on("removedfile", function (file) {
                            var acceptedFiles = addFilesToProjectFolderDropzone.getAcceptedFiles();
                            if (acceptedFiles.length) {
                                removeAllFilesButton.style.display = 'block';
                                submitButton.style.display = 'inline-block';
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                submitButton.style.display = 'none';
                            }
                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                type: 'POST',
                                data: {action: "ADD_FILES", subAction: "REMOVE_FILE", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                    tmpFolderName: $('#modalAddFilesToProjectFolder-temp-folder-name').val(), fileName: file.name},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function (data) {
                                    console.log("delete tmp file success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    },
                    clean: function () {
                        addFilesToProjectFolderDropzone.removeAllFiles(true);
                    }
                };
                $("#addFilesToProjectFolderDropzone").dropzone(Dropzone.options.addFilesToProjectFolderDropzone);
                // on modal closed remove tmp folder
                $('#modalAddFilesToProjectFolder').on('hidden.bs.modal', function () {
                    // clean the dropzone
                    Dropzone.options.addFilesToProjectFolderDropzone.clean();
                    // remove the temporary file from server
                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        type: 'POST',
                        data: {action: "ADD_FILES", subAction: "REMOVE_TMP_FOLDER", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                            tmpFolderName: $('#modalAddFilesToProjectFolder-temp-folder-name').val()},
                        xhrFields: {
                            withCredentials: true
                        },
                        crossDomain: true,
                        success: function (data) {
                            console.log("delete tmp folder success");
                            $('#modalAddFilesToProjectFolder-temp-folder-name').val('');
                            $('#modalAddFilesToProjectFolder-target-nodeId').val('');
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        }
                    });
                    // clean submodal
                    $('#add-new-files-duplicates-list').html('');
                    $('#add-new-files-duplicates-submodal').submodal('hide');
                });

                // global dropzone variable for replace file modal
                Dropzone.options.replaceFileDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 500,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    clean: function () {
                        Dropzone.forElement("#replaceFileDropzone").removeAllFiles(true);
                    },

                    init: function () {
                        replaceFileDropzone = this;

                        // Setup the event listener for replaceFileSubmitButton button
                        var replaceFileSubmitButton = document.querySelector("button#modalReplaceFile-submit");
                        replaceFileSubmitButton.addEventListener('click', function (event) {

                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                type: 'POST',
                                data: {action: "REPLACE_FILE", subAction: "SUBMIT", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                    tmpFolderName: $('#modalReplaceFile-temp-folder-name').val(),
                                    nodeId: $('#modalReplaceFile-target-nodeId').val()},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                timeout: 60000,
                                beforeSend: function () {
                                    showBusysign();
                                },
                                success: function (data) {
                                    var tree = $("#jstree-tree-fileeditor");
                                    var nodeId = $('#modalReplaceFile-target-nodeId').val();

                                    // check opened tabs if contains an updated node, if so => update its content
                                    if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                        for (var p = 0; p < w2ui['tabs_file_editor'].tabs.length; p++) {
                                            var tmpNodeId = w2ui['tabs_file_editor'].tabs[p].id;
                                            var tmpNode = tree.jstree().get_node(tmpNodeId);

                                            if (String(tmpNodeId) === String(nodeId)) { // tab is opened, its content has been replaces => must refresh the tab content
                                                console.log("updating tab : " + tmpNode.text);
                                                // get file content from server
                                                $.ajax({
                                                    url: "/api/protected/getFileContent",
                                                    data: {nodeId: tmpNodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                                    method: 'GET',
                                                    dataType: "json",
                                                    timeout: 30000,
                                                    cache: false,
                                                    success: function (data) {
                                                        projectEditor.fileWrapper.updateTabContent(data);
                                                    },
                                                    error: function (xhr) {
                                                        var msg;
                                                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                            msg = xhr.statusText;
                                                        } else {
                                                            msg = xhr.statusText + ": " + xhr.responseText;
                                                        }
                                                        notify(msg, "error", 7000);
                                                    }
                                                });
                                                break;
                                            }
                                        }
                                    }
                                    // close the modal
                                    $('#modalReplaceFile').modal('hide');
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                complete: function () {
                                    hideBusysign();
                                }
                            });
                        });

                        // Setup the event listener for removeAllFilesButton button
                        var removeAllFilesButton = document.querySelector("button#clear-replace-file-dropzone");
                        removeAllFilesButton.addEventListener('click', function (event) {
                            // cancel uploads as well, call removeAllFiles(true);
                            replaceFileDropzone.removeAllFiles(true);
                        });

                        this.on('maxfilesexceeded', function (file) {
                            console.log('max files exceeded => removing file!');
                        });


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                            var tree = $('#jstree-tree-fileeditor');
                            var nodeId = $('#modalReplaceFile-target-nodeId').val();
                            var node = tree.jstree().get_node(nodeId);
                            var re = /(?:\.([^./]+))?$/;
                            var acceptedExtension = re.exec(node.text)[1];

                            var fileExt = re.exec(file.name)[1];

                            if (acceptedExtension !== undefined && fileExt !== acceptedExtension) {
                                this.removeFile(file);
                                notify("File extension must be the same as the original => " + acceptedExtension, "error", 5000);
                            }

                            // Show/hide buttons here
                            if (replaceFileDropzone.files.length) {
                                removeAllFilesButton.style.display = 'block';
                                replaceFileSubmitButton.style.display = 'inline-block';
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                replaceFileSubmitButton.style.display = 'none';
                            }
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone "replace file" processing');
                            this.options.url = "/api/protected/contextMenuForEditor?action=REPLACE_FILE&subAction=UPLOAD_FILE&projectUuid=" +
                                $('#project-editor-devenvironment-data').attr('data-project-uuid') + "&tmpFolderName=" +
                                $('#modalReplaceFile-temp-folder-name').val() + "&fileName=" + file.name;
                        });

                        this.on("sending", function (file, xhr, formData) {
                            console.log('Sending the file' + file.name);
                            formData.append('uploaded_file', file);
                        });

                        this.on('success', function (file, response) {
                            console.log("'replace file' upload done with success");
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 7000);
                            this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone thumbnail');
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            var acceptedFiles = replaceFileDropzone.getAcceptedFiles();
                            if (acceptedFiles.length) {
                                removeAllFilesButton.style.display = 'block';
                                replaceFileSubmitButton.style.display = 'inline-block';
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                replaceFileSubmitButton.style.display = 'none';
                            }
                            // remove file from server
                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                type: 'POST',
                                data: {action: "REPLACE_FILE", subAction: "REMOVE_FILE", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                    tmpFolderName: $('#modalReplaceFile-temp-folder-name').val(), fileName: file.name},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function (data) {
                                    console.log("delete tmp file success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#replaceFileDropzone").dropzone(Dropzone.options.replaceFileDropzone);
                // on modal closed remove tmp folder
                $('#modalReplaceFile').on('hidden.bs.modal', function () {
                    // clean the dropzone
                    Dropzone.options.replaceFileDropzone.clean();
                    // remove the temporary file from server
                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        type: 'POST',
                        data: {action: "REPLACE_FILE", subAction: "REMOVE_TMP_FOLDER", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                            tmpFolderName: $('#modalReplaceFile-temp-folder-name').val()},
                        xhrFields: {
                            withCredentials: true
                        },
                        crossDomain: true,
                        success: function (data) {
                            console.log("delete tmp folder success");
                            $('#modalReplaceFile-temp-folder-name').val('');
                            $('#modalReplaceFile-target-nodeId').val('');
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        }
                    });
                });


                // global dropzone variable for app icon modifier
                Dropzone.options.appIconModifierDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: false,
                    paramName: "uploaded_file",
                    maxFilesize: 5,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    maxFiles: 1,
                    acceptedFiles: ".png",
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: true,
                    withCredentials: true, // for CORS

                    clean: function () {
                        Dropzone.forElement("#appIconModifierDropzone").removeAllFiles(true);
                    },

                    // Instead of directly accepting / rejecting the file, setup two
                    // functions on the file that can be called later to accept / reject
                    // the file.
                    accept: function (file, done) {
                        file.acceptDimensions = function () {
                            done();
                        };
                        file.rejectDimensions = function () {
                            done("Bad dimensions, image must be 512x512");
                        };
                        // Of course you could also just put the `done` function in the file
                        // and call it either with or without error in the `thumbnail` event
                        // callback, but I think that this is cleaner.
                    },

                    init: function () {
                        appIconModifierDropzone = this;

                        // Setup the event listener for modifyAppIconSubmitButton button
                        var modifyAppIconSubmitButton = document.querySelector("button#modalModifyAppIcon-submit");
                        modifyAppIconSubmitButton.addEventListener('click', function (event) {
                            if (!appIconModifierDropzone.getAcceptedFiles().length) {
                                notify('You must add at least one file before submitting!', 'warning', 5000);
                            } else if (appIconModifierDropzone.getUploadingFiles().length) {
                                notify('Please wait for files to upload', 'warning', 5000);
                            } else {
                                console.log('Submitting APP ICON modifier...' + appIconModifierDropzone.savedTmpFilePath);
                                $.ajax({
                                    url: "/api/protected/toolsOfEditor",
                                    type: 'POST',
                                    data: {action: "SUBMIT_APP_ICON_MODIFIER",
                                        projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                        savedTmpFilePath: encodeURIComponent(Dropzone.options.appIconModifierDropzone.savedTmpFilePath)},
                                    xhrFields: {
                                        withCredentials: true
                                    },
                                    crossDomain: true,
                                    timeout: 60000,
                                    beforeSend: function () {
                                        showBusysign();
                                    },
                                    success: function (data) {
                                        console.log(JSON.stringify(data));
                                        if (data.length > 0) {
                                            var tree = $("#jstree-tree-fileeditor");
                                            // check opened tabs if contains an updated node, if so => update its content
                                            if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                                $.each(data, function (index) {
                                                    var nodeId = data[index];
                                                    var fileIsOpened = false;
                                                    var listItems = $("ul#fe-main-list-tab-content > li");
                                                    listItems.each(function (idx, li) {
                                                        if ($(li).attr("data-node-id") === nodeId) {
                                                            fileIsOpened = true;
                                                            return false;
                                                        }
                                                    });
                                                    if (fileIsOpened) { // tab already opened => update its content
                                                        // get file content from server
                                                        $.ajax({
                                                            url: "/api/protected/getFileContent",
                                                            data: {nodeId: nodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                                            method: 'GET',
                                                            dataType: "json",
                                                            timeout: 30000,
                                                            cache: false,
                                                            success: function (data) {
                                                                // update editor with new content
                                                                projectEditor.fileWrapper.updateTabContent(data);
                                                            },
                                                            error: function (xhr) {
                                                                var msg;
                                                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                                    msg = xhr.statusText;
                                                                } else {
                                                                    msg = xhr.statusText + ": " + xhr.responseText;
                                                                }
                                                                notify(msg, "error", 7000);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                            notify(data.length + " icon(s) modified with success", "success", 5000);
                                        } else {
                                            notify("Oops nothing has changed!", "information", 5000);
                                        }
                                        // close the modal
                                        $('#modalModifyAppIcon').modal('hide');
                                    },
                                    error: function (xhr) {
                                        var msg;
                                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                                            msg = xhr.statusText;
                                        } else {
                                            msg = xhr.statusText + ": " + xhr.responseText;
                                        }
                                        notify(msg, "error", 7000);
                                    },
                                    complete: function () {
                                        hideBusysign();
                                    }
                                });
                            }
                        });

                        // Setup the event listener for removeAllFilesButton button
                        var removeAllFilesButton = document.querySelector("button#clear-appIconModifier-dropzone");
                        removeAllFilesButton.addEventListener('click', function (event) {
                            // cancel uploads as well, call removeAllFiles(true);
                            appIconModifierDropzone.removeAllFiles(true);
                        });

                        this.on('maxfilesexceeded', function (file) {
                            console.log('max files exceeded => removing file!');
                        });


                        this.on("drop", function (event) {
                            console.log('Dropzone on drop');
                        });

                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                            if (appIconModifierDropzone.files.length) {
                                removeAllFilesButton.style.display = 'block';
                                modifyAppIconSubmitButton.style.display = 'inline-block';
                                //appIconModifierDropzone.processQueue();
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                modifyAppIconSubmitButton.style.display = 'none';
                            }
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone on thumbnail');
                            if (appIconModifierDropzone.files.length === 1) {
                                // Do the dimension checks you want to do
                                if (file.width != 512 || file.height != 512) {
                                    file.rejectDimensions()
                                } else {
                                    file.acceptDimensions();
                                    appIconModifierDropzone.processQueue();
                                }
                            }
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone processing');
                            if (appIconModifierDropzone.files.length === 1) {
                                console.log('Dropzone "APP ICON MODIFIER" processing');
                                this.options.url = "/api/protected/uploadTmpFile";
                            }
                        });

                        this.on('success', function (file, response) {
                            console.log("APP ICON MODIFIER done with success");
                            Dropzone.options.appIconModifierDropzone.savedTmpFilePath = decodeURIComponent(response);
                            console.log("savedTmpFilePath = " + Dropzone.options.appIconModifierDropzone.savedTmpFilePath);
                        });


                        // On error show a notification for 5 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 5000);
                            //if (!file.accepted) {
                            this.removeFile(file);
                            //}
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone removedFile');
                            var acceptedFiles = appIconModifierDropzone.getAcceptedFiles();
                            if (acceptedFiles.length) {
                                removeAllFilesButton.style.display = 'block';
                                modifyAppIconSubmitButton.style.display = 'inline-block';
                            } else {
                                removeAllFilesButton.style.display = 'none';
                                modifyAppIconSubmitButton.style.display = 'none';
                            }


                            // ajax delete tmp file
                            if (!Dropzone.options.appIconModifierDropzone.savedTmpFilePath)
                                return;

                            $.ajax({
                                url: "/api/protected/deleteTmpFileHandler" + "?savedTmpFilePath=" + encodeURIComponent(Dropzone.options.appIconModifierDropzone.savedTmpFilePath),
                                type: 'DELETE',
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function () {
                                    Dropzone.options.appIconModifierDropzone.savedTmpFilePath = null;
                                    console.log("deleted file from server success");
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#appIconModifierDropzone").dropzone(Dropzone.options.appIconModifierDropzone);
                // on modal closed remove tmp folder if exists
                $('#modalModifyAppIcon').on('hidden.bs.modal', function () {
                    // clean the dropzone
                    Dropzone.options.appIconModifierDropzone.clean();
                });


                // modal create new keystore on hide => reset fields
                $('#modalCreateKeystore').on('hidden.bs.modal', function () {
                    // hide submoldal
                    $('#new-keystore-properties-submodal').submodal('hide');

                    // reset fields
                    $('#new_keystore_alias').val('');
                    $('#new_keystore_kspwd').val('');
                    $('#new_keystore_keypwd').val('');
                    $('#new_keystore_filename').val('');
                    $('#new_keystore_validity').val('');
                    $('#new_keystore_desc').val('');

                    $('#new_keystore_cn').val('');
                    $('#new_keystore_ou').val('');
                    $('#new_keystore_o').val('');
                    $('#new_keystore_l').val('');
                    $('#new_keystore_st').val('');
                    $('#new_keystore_c').val('');
                    $('#new_keystore_e').val('');
                });

                $('#modalKeystoreDetails').on('hidden.bs.modal', function () {
                    $('#keystore-details-container').html('');
                });


                // global dropzone variable for modal keystore import
                Dropzone.options.importKeystoreDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    autoProcessQueue: true,
                    paramName: "uploaded_file",
                    maxFilesize: 2,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    acceptedFiles: ".ks,.jks,.keystore,",
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,
                    withCredentials: true, // for CORS

                    clean: function () {
                        Dropzone.forElement("#importKeystoreDropzone").removeAllFiles(true);
                    },

                    init: function () {
                        importKeystoreDropzone = this;

                        var checkKeystorePwdButton = document.querySelector("button#import-keystore-check-password");
                        checkKeystorePwdButton.addEventListener('click', function (event) {
                            if (!Dropzone.options.importKeystoreDropzone.savedTmpFilePath) {
                                notify("Please upload a keystore file!", "error", 5000);
                                return;
                            }
                            var ksPwd = $('#import-keystore-kspwd').val();
                            if (!ksPwd) {
                                notify("Please enter a keystore password!", "error", 5000);
                                return;
                            }

                            // Ajax validate keystore with its password
                            $.ajax({
                                url: "/api/protected/keytool",
                                data: {action: "VALIDATE_KEYSTORE", tmpFilePath: Dropzone.options.importKeystoreDropzone.savedTmpFilePath, ksPwd: ksPwd},
                                method: 'POST',
                                dataType: "json",
                                timeout: 15000,
                                cache: false,
                                beforeSend: function () {
                                    showBusysign();
                                },
                                success: function (data) {
                                    // populate mustache template
                                    console.log('info: ' + JSON.stringify(data.info));

                                    if (data.info.length > 0) {
                                        var template = $('#apkreverse-keystore-import-valid-details-template').html();
                                        var dataTemplate = {
                                            info: data.info
                                        };
                                        var html = Mustache.to_html(template, dataTemplate);
                                        $("div#import-keystore-validation-details").html(html);
                                        // max length plugin
                                        $('.max-length').maxlength({
                                            alwaysShow: true,
                                            appendToParent: true
                                        });
                                    } else {
                                        notify("This keystore does not contain any certificate!", "warning", 5000);
                                    }
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                    $("div#import-keystore-validation-details").html('');
                                },
                                complete: function () {
                                    hideBusysign();
                                }
                            });

                        });


                        this.on('maxfilesexceeded', function (file) {
                            console.log('max files exceeded => removing file!');
                        });


                        this.on("drop", function (event) {
                            //console.log(myDropzone.files);
                            console.log('Dropzone on drop');
                        });

                        // You might want to show the submit button only when
                        // files are dropped here:
                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                        });

                        this.on("processing", function (file) {
                            console.log('Dropzone processing');
                            this.options.url = "/api/protected/uploadTmpFile";
                        });

                        this.on('success', function (file, response) {
                            console.log('Dropzone success');
                            Dropzone.options.importKeystoreDropzone.savedTmpFilePath = decodeURIComponent(response);
                            console.log("savedTmpFilePath = " + Dropzone.options.importKeystoreDropzone.savedTmpFilePath);
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone keystore error');
                            notify(message, 'error', 7000);
                            if (!file.accepted)
                                this.removeFile(file);
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone thumbnail');
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            // reset UI

                            // ajax delete tmp file
                            if (!Dropzone.options.importKeystoreDropzone.savedTmpFilePath)
                                return;

                            $.ajax({
                                url: "/api/protected/deleteTmpFileHandler" + "?savedTmpFilePath=" + encodeURIComponent(Dropzone.options.importKeystoreDropzone.savedTmpFilePath),
                                type: 'DELETE',
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function () {
                                    Dropzone.options.importKeystoreDropzone.savedTmpFilePath = null;
                                    console.log("delete file from server success");
                                    $('#import-keystore-filename-div').html('');
                                    $("div#import-keystore-validation-details").html('');
                                    $('#import-keystore-kspwd').val('');
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        });
                    }};
                $("#importKeystoreDropzone").dropzone(Dropzone.options.importKeystoreDropzone);
                $('#modalImportKeystore').on('hidden.bs.modal', function () {
                    // remove the temporary file from server and clean the dropzone
                    Dropzone.options.importKeystoreDropzone.clean();
                    // clean modal
                    $("div#import-keystore-validation-details").html('');
                    $('#import-keystore-kspwd').val('');
                    // clean submodal
                    $('#import-keystore-filename-div').html('');
                    $('#import-keystore-filename-submodal').submodal('hide');

                });

                // clean apk info modal on hide
                $('#modalApkInfo').on('hidden.bs.modal', function () {
                    $('div#apk-info-container').html('');
                });

                // folder explorer bind event
                $(document).on("click", "#folder-explorer-up", function (e) {
                    try {
                        $.playSound('/static/public/sounds/file-click.mp3');
                    } catch (err) {
                        // do nothing
                    }
                    projectEditor.folderExplorer.showInExplorer($('#folder-explorer-up').attr('data-up-folder'));
                });

                $(document).on("dblclick", ".exp-img-cont", function (e) {
                    var nodeToOpen_id = $(this).attr('data-node-id');
                    var nodeToOpen_label = $(this).attr('data-node-label');
                    console.log('clicked on id: ' + nodeToOpen_id);

                    if (nodeToOpen_label === 'directory') {
                        try {
                            $.playSound('/static/public/sounds/file-click.mp3');
                        } catch (err) {
                            // do nothing
                        }
                        projectEditor.folderExplorer.showInExplorer(nodeToOpen_id);
                    } else {
                        console.log('opening file id: ' + nodeToOpen_id);
                        var nodeToOpen_type = $(this).attr('data-node-type');
                        $('#modalFolderExplorer').modal('hide');
                        // open file in editor
                        projectEditor.tabsManager.openTabFromExplorer(nodeToOpen_id, nodeToOpen_type, $('#project-editor-devenvironment-data').attr('data-project-uuid'))
                    }
                });

                // clean folder explorer modal on hide
                $('#modalFolderExplorer').on('hidden.bs.modal', function () {
                    var pos = $('#filter-positive-folder-exp-input').val();
                    var neg = $('#filter-negative-folder-exp-input').val();
                    if (pos) {
                        projectEditor.folderExplorer.last_positive_filter = pos;
                    } else {
                        projectEditor.folderExplorer.last_positive_filter = null;
                    }

                    if (neg) {
                        projectEditor.folderExplorer.last_negative_filter = neg;
                    } else {
                        projectEditor.folderExplorer.last_negative_filter = null;
                    }
                    projectEditor.folderExplorer.cleanExplorerModal();
                });


                $("#filter-positive-folder-exp-input").keyup(function (e) {
                    projectEditor.folderExplorer.filterExplorerDebounce(this, e);
                });

                // folder explorer modal negative filter
                $("#filter-negative-folder-exp-input").keyup(function (e) {
                    projectEditor.folderExplorer.filterExplorerDebounce(this, e);
                });


            },
            openNewProjectSubModal: function () {
                // init project name input as filename without extension
                var $new_project_name_input = $("#new_project_name_input");
                //$new_project_name_input.val(Dropzone.options.apkNewProjectDropzone.currentFileName.replace(/\.[^/.]+$/, ""));
                $new_project_name_input.val($('span#new-project-info-name').text());
                // show submodal
                $('#new-project-name-submodal').submodal('show');
                $new_project_name_input.focus();
            },
            getUserProjectsTotalSize: function () {
                console.log("get user projects total size...")
                // reset old total size
                $("span#user-projects-total-size-txt").html('');
                // show loader image
                $("img#user-projects-total-size-loader").show();
                // websocket get total projects size
                var message = {"type": "INSTRUCTION", content: "GET_USER_PROJECTS_TOTAL_SIZE"};
                RevEnge.sendWebSocketMessage(JSON.stringify(message));
                // let webSocketWrapper.webSocketMessagingProtocol take care of the rest
            }
        };

        var keystoreListWrapper = {
            reloadUiKeystoresList: function () {
                // load projects json from server and populate mustache template
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "GET_ALL"},
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        $("div#apkreverse-keystore-list").html('<div class="col-xs-12"><h1>Loading keystores...</h1></div>');
                    },
                    success: function (data) {
                        // populate mustache template
                        var template = $('#apkreverse-keystores-list-template').html();
                        var dataTemplate = {
                            keystores: data
                        };
                        var html = Mustache.to_html(template, dataTemplate);
                        $("div#apkreverse-keystore-list").html(html);
                        // set keystores filter by name and package and description => only if the user have one keystore at least.
                        if ($.isArray(data) && data.length > 0) {
                            // set filter on keystores table
                            $('input#keystores-list-filter-input').keyup(function (e) {
                                var code = e.keyCode || e.which;
                                if (code == '9') return;
                                if (code == '27') $(this).val(null);
                                var $rows = $('table#keystores-list-table').find('tbody tr');
                                var filterVal = $.trim($(this).val()).replace(/ +/g, ' ').toLowerCase();
                                $rows.removeClass('hidden').filter(function () {
                                    var text = $(this).find("td[data-type='filter']").text().replace(/\s+/g, ' ').toLowerCase();
                                    return !~text.indexOf(filterVal);
                                }).addClass('hidden');
                            });
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        $("div#apkreverse-keystore-list").html('<div class="col-xs-12"><h3><a href="#" onclick="event.preventDefault(); ApkToolModule.reloadUiKeystoresList();">Click here to reload keystores</a></h3></div>');
                    },
                    complete: function () {

                    }
                });
            },
            openCreateKeystoretModal: function (e) {
                e.preventDefault();
                // show modal
                $('#modalCreateKeystore').modal('show');
            },
            openImportKeystoreModal: function (e) {
                e.preventDefault();
                // show modal
                $('#modalImportKeystore').modal('show');
            },
            newKeystoreNextStep: function () {
                var alias = $('#new_keystore_alias').val();
                var kspwd = $('#new_keystore_kspwd').val();
                var keypwd = $('#new_keystore_keypwd').val();
                var validity = $('#new_keystore_validity').val();
                var description = $('#new_keystore_desc').val();


                // check required fileds are not empty
                if (!alias) {
                    notify("Alias is required!", "error", 5000);
                    return;
                }
                if (!kspwd) {
                    notify("Keystore password is required!", "error", 5000);
                    return;
                }
                if (!keypwd) {
                    notify("Private key password is required!", "error", 5000);
                    return;
                }
                if (!validity) {
                    notify("Certificate validity is required!", "error", 5000);
                    return;
                }

                // ajax check if filename already exists, and validate it
                // load projects json from server and populate mustache template
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "VALIDATE_FORM", alias: alias, kspwd: kspwd, keypwd: keypwd, validity: validity, description: description},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // open submodal
                        $('#new-keystore-properties-submodal').submodal('show');
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            createNewKeystore: function () {
                var alias = $('#new_keystore_alias').val();
                var kspwd = $('#new_keystore_kspwd').val();
                var keypwd = $('#new_keystore_keypwd').val();
                var description = $('#new_keystore_desc').val();
                var validity = $('#new_keystore_validity').val();

                // check required fileds are not empty
                if (!alias) {
                    notify("Alias is required!", "error", 5000);
                    return;
                }
                if (!kspwd) {
                    notify("Keystore password is required!", "error", 5000);
                    return;
                }
                if (!keypwd) {
                    notify("Private key password is required!", "error", 5000);
                    return;
                }
                if (!validity) {
                    notify("Certificate validity is required!", "error", 5000);
                    return;
                }

                // ajax check if filename already exists, and validate it
                // load projects json from server and populate mustache template
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "SUBMIT_CREATE_KEYSTORE", alias: alias, kspwd: kspwd, keypwd: keypwd, validity: validity, description: description,
                        CN: $('#new_keystore_cn').val(), OU: $('#new_keystore_ou').val(), O: $('#new_keystore_o').val(),
                        L: $('#new_keystore_l').val(), ST: $('#new_keystore_st').val(), C: $('#new_keystore_c').val(), E: $('#new_keystore_e').val()},
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        $('#new-keystore-properties-submodal').submodal('hide');
                        $('#modalCreateKeystore').modal('hide');
                        keystoreListWrapper.reloadUiKeystoresList();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            keystoreActions: function (e, element, action) {
                e.preventDefault();
                var keystoreUuid = $(element).closest('tr').attr('data-keystrore-uuid');
                switch (action) {
                    case "details":
                    {
                        keystoreListWrapper.keystoreDetails(keystoreUuid);
                        break;
                    }
                    case "download":
                    {
                        keystoreListWrapper.keystoreDownload(keystoreUuid);
                        break;
                    }
                    case "update":
                    {
                        keystoreListWrapper.keystoreUpdate(keystoreUuid);
                        break;
                    }
                    case "remove":
                    {
                        var keystoreAlias = $(element).closest('tr').attr('data-keystrore-alias');
                        keystoreListWrapper.keystoreRemove(keystoreUuid, keystoreAlias);
                        break;
                    }
                }
            },
            keystoreDetails: function (keystoreUuid) {
                console.log('details for keystore : ' + keystoreUuid);
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "DETAILS_KEYSTORE", keystoreUuid: keystoreUuid},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // populate mustache template and open info modal containing the received data
                        //console.log(JSON.stringify(data));

                        // populate mustache template
                        var template = $('#apkreverse-keystore-details-template').html();
                        var dataTemplate = {
                            properties: data.properties,
                            details: data.details
                        };
                        var html = Mustache.to_html(template, dataTemplate);

                        $('#keystore-details-container').html(html);
                        $('#modalKeystoreDetails').modal('show');

                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            keystoreDownload: function (keystoreUuid) {
                console.log('download keystore : ' + keystoreUuid);
                try {
                    // remove previous iframe
                    var previousDownloadFrame = document.getElementById('iframe-fe-file-download');
                    if (previousDownloadFrame !== null) {
                        previousDownloadFrame.parentNode.removeChild(previousDownloadFrame);
                    }
                    // add new iframe
                    var downloadFrame = document.createElement("iframe");
                    downloadFrame.id = 'iframe-fe-file-download';
                    downloadFrame.setAttribute('class', "downloadFrameScreen");
                    var url = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port
                        + "/api/protected/downloadKeystoreFile?keystoreUuid=" + keystoreUuid;
                    downloadFrame.setAttribute('src', url);
                    document.body.appendChild(downloadFrame);
                } catch (err) {
                    notify(err.toString(), "error", 7000);
                }
            },
            keystoreUpdate: function (keystoreUuid) {
                //console.log('Update keystore : ' + keystoreUuid);
                // ajax get html
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "GET_MODAL_HTML_UPDATE_KEYSTORE", keystoreUuid: keystoreUuid},
                    method: 'POST',
                    timeout: 20000,
                    dataType: "text",
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var $container = $('#myAbstractAjaxModalContent');
                        $container.empty();
                        $container.html(data);
                        $('#myAbstractAjaxModal').modal('show');
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            keystoreSubmitUpdate: function () {
                var keystoreUuid = $('#ks_uuid_update').val();
                var ksDescription = $('#ks_description_update').val();
                if (!ksDescription) {
                    notify("Please enter your new description!", "error", 5000);
                    return;
                }
                if (ksDescription.length < 10 || ksDescription.length > 200) {
                    notify("Keystore description must contain between 10 and 200 characters!", "error", 5000);
                    return;
                }
                $.ajax({
                    url: "/api/protected/keytool",
                    data: {action: "SUBMIT_UPDATE_KEYSTORE", keystoreUuid: keystoreUuid, ksDescription: ksDescription},
                    method: 'POST',
                    timeout: 20000,
                    dataType: "text",
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        notify("Keystore description updated successfully!", "success", 5000);
                        $('#myAbstractAjaxModal').modal('hide');
                        // reload keystore list
                        keystoreListWrapper.reloadUiKeystoresList();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            keystoreRemove: function (keystoreUuid, keystoreAlias) {
                console.log('Removekeystore : ' + keystoreUuid);

                w2confirm('Do want to delete keystore withe alias "' + keystoreAlias + '"?')
                    .yes(function () {
                        $.ajax({
                            url: "/api/protected/keytool",
                            data: {action: "REMOVE_KEYSTORE", keystoreUuid: keystoreUuid},
                            method: 'POST',
                            dataType: "json",
                            timeout: 15000,
                            cache: false,
                            beforeSend: function () {
                                showBusysign();
                            },
                            success: function (data) {
                                notify("Keystore '" + keystoreAlias + "' removed with success", "success", 5000);
                                keystoreListWrapper.reloadUiKeystoresList();

                            },
                            error: function (xhr) {
                                var msg;
                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                    msg = xhr.statusText;
                                } else {
                                    msg = xhr.statusText + ": " + xhr.responseText;
                                }
                                notify(msg, "error", 7000);
                            },
                            complete: function () {
                                hideBusysign();
                            }
                        });
                    })
                    .no(function () {
                    });
            },
            importKeystore: function () {
                if (!Dropzone.options.importKeystoreDropzone.savedTmpFilePath) {
                    notify("Please upload a keystore file!", "error", 5000);
                    return;
                }
                var ksPwd = $('#import-keystore-kspwd').val();
                if (!ksPwd) {
                    notify("Please enter a keystore password!", "error", 5000);
                    return;
                }

                var alias = $('#import-keystore-aliases-select').val();
                if (!alias) {
                    notify("Please select an alias!", "error", 5000);
                    return;
                }

                var keyPwd = $('#import-keystore-keypwd').val();
                if (!keyPwd) {
                    notify("Please enter a private key password!", "error", 5000);
                    return;
                }

                var description = $('#import-keystore-description').val();

                var ajax_data = {action: "SUBMIT_IMPORT_KEYSTORE", tmpFilePath: Dropzone.options.importKeystoreDropzone.savedTmpFilePath,
                    ksPwd: ksPwd, alias: alias, keyPwd: keyPwd, description: description};


                // Ajax submit import keystore keystore
                $.ajax({
                    url: "/api/protected/keytool",
                    data: ajax_data,
                    method: 'POST',
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data, textStatus, xhr) {
                        //console.log("success for chunk...." + xhr.status);
                        switch (xhr.status) {
                            case 204:
                                // success import
                                notify("Keystore imported with success", "success", 5000);
                                // hide modal
                                $('#modalImportKeystore').modal('hide');
                                // reload keystores list
                                keystoreListWrapper.reloadUiKeystoresList();
                                break;
                            case 200:
                                // filename already exists ask for confirmation
                                $('#import-keystore-filename-div').html('A keystore file named "' + data + '" already exists!<br>Click "Rename" to continue?');
                                $('#import-keystore-filename-submodal').submodal('show');
                                break;
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            }
        };


        // projects list wrapper
        var projectsListWrapper = {
            backToProjectsList: function () {
                $.ajax({
                    url: "/api/protected/closeProject",
                    data: {action: "TRY_CLOSE"},
                    method: 'POST',
                    timeout: 15000,
                    dataType: "text",
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data, textStatus, xhr) {
                        switch (xhr.status) {
                            case 204:
                            {
                                // reload projects
                                projectsListWrapper.reloadUiProjectsList($('#project-editor-devenvironment-data').attr('data-project-uuid'));
                                // show project editor tab
                                $('ul#projects-hidden-navtab a[href="#tab-apkreverse-projects-list"]').tab('show');
                                // destroy project editor
                                projectEditor.destroy();
                                break;
                            }
                            case 200:
                            {
                                w2confirm(data)
                                    .yes(function () {
                                        // ajax force stop all ongoing processes related to project editor
                                        $.ajax({
                                            url: "/api/protected/closeProject",
                                            data: {action: "FORCE_CLOSE"},
                                            method: 'POST',
                                            timeout: 15000,
                                            dataType: "text",
                                            cache: false,
                                            beforeSend: function () {
                                                showBusysign();
                                            },
                                            success: function (data) {
                                                // reload projects
                                                projectsListWrapper.reloadUiProjectsList($('#project-editor-devenvironment-data').attr('data-project-uuid'));
                                                // show project editor tab
                                                $('ul#projects-hidden-navtab a[href="#tab-apkreverse-projects-list"]').tab('show');
                                                // destroy project editor
                                                projectEditor.destroy();
                                            },
                                            error: function (xhr) {
                                                var msg;
                                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                    msg = xhr.statusText;
                                                } else {
                                                    msg = xhr.statusText + ": " + xhr.responseText;
                                                }
                                                notify(msg, "error", 7000);
                                            },
                                            complete: function () {
                                                hideBusysign();
                                            }
                                        });
                                    })
                                    .no(function () {
                                    });
                                break;
                            }
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            reloadUiProjectsList: function (lastOpenedProjectUuid) {
                // load projects json from server and populate mustache template and update last opened project's app icon
                var url = "/api/protected/getAllApkProjects";
                if (lastOpenedProjectUuid) {
                    url = "/api/protected/getAllApkProjects?lastOpenedProjectUuid=" + lastOpenedProjectUuid;
                }
                $.ajax({
                    url: url,
                    data: {},
                    method: 'GET',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        $("div#tab-apkreverse-projects-list").html('<div class="col-xs-12"><h1>Loading projects...</h1></div>');
                    },
                    success: function (data) {
                        // populate mustache template
                        var template = $('#apkreverse-projects-list-template').html();
                        var dataTemplate = {
                            projects: data
                        };
                        var html = Mustache.to_html(template, dataTemplate);
                        $("div#tab-apkreverse-projects-list").html(html);
                        // set project filter by name and package and get total projects size => only if the user have one project at least.
                        if ($.isArray(data) && data.length > 0) {
                            ApkToolModule.getUserProjectsTotalSize();
                            // set filter on projects table
                            $('input#projects-list-filter-input').keyup(function (e) {
                                var code = e.keyCode || e.which;
                                if (code == '9') return;
                                if (code == '27') $(this).val(null);
                                var $rows = $('table#projects-list-table').find('tbody tr');
                                var filterVal = $.trim($(this).val()).replace(/ +/g, ' ').toLowerCase();
                                $rows.removeClass('hidden').filter(function () {
                                    var text = $(this).find("td[data-type='filter']").text().replace(/\s+/g, ' ').toLowerCase();
                                    return !~text.indexOf(filterVal);
                                }).addClass('hidden');
                            });
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        $("div#tab-apkreverse-projects-list").html('<div class="col-xs-12"><h3><a href="#" onclick="event.preventDefault(); ApkToolModule.reloadUiProjectsList();">Click here to reload projects</a></h3></div>');
                    }
                });
            },
            createNewProject: function () {
                var savedTmpApkFilePath = Dropzone.options.apkNewProjectDropzone.savedTmpFilePath;
                var $projectNameInput = $('input#new_project_name_input');
                var projectName = $projectNameInput.val();
                if (!savedTmpApkFilePath) {
                    notify("Error, no apk file found, please try again!", "error", 5000);
                    return;
                }

                if (!projectName) {
                    notify("Please enter a project name!", "error", 5000);
                    $projectNameInput.focus();
                    return;
                } else {
                    if (projectName.length < 5) {
                        notify("Project name must contain least 5 characters!", "error", 5000);
                        $projectNameInput.focus();
                        return;
                    } else if (projectName.length > 30) {
                        notify("Project name must not exceed 30 characters!", "error", 5000);
                        $projectNameInput.focus();
                        return;
                    }
                }

                // ajax request create new project
                console.log("create new project : " + projectName + ", Apk file path: " + savedTmpApkFilePath);
                // ajax create new project
                $.ajax({
                    url: "/api/protected/createNewProject",
                    data: {apkTmpFilePath: savedTmpApkFilePath, projectName: projectName},
                    type: 'POST',
                    timeout: 10000,
                    success: function (data) {
                        // hide modal
                        $('#modalCreateProject').modal('hide');
                        // webSocketWrapper.webSocketMessagingProtocol will take care of the rest
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    }
                });
            },
            openProject: function (e, clickedElement) {
                e.preventDefault();
                var projectUuid = $(clickedElement).closest('tr').attr('data-project-uuid');
                projectsListWrapper.open_project_(projectUuid);
            },
            open_project_: function (projectUuid) {
                console.log('open project : ' + projectUuid);
                // ajax load project
                $.ajax({
                    url: "/api/protected/loadProjectForEditor",
                    data: {projectUuid: projectUuid},
                    type: 'GET',
                    timeout: 15000,

                    beforeSend: function () {
                        // freeze UI until project data is loaded
                        $.blockUI({
                            message: "Opening project. Please wait...",
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .5,
                                color: '#fff'
                            } });
                    },
                    success: function (data) {
                        // populate project editor template
                        projectEditor.init(data);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        $.unblockUI();
                    }
                });
            },
            cloneProject: function (e, clickedElement) {
                e.preventDefault();
                var projectUuid = $(clickedElement).closest('tr').attr('data-project-uuid');
                console.log('clone project : ' + projectUuid);
            },
            detailsProject: function (e, clickedElement) {
                e.preventDefault();
                var projectUuid = $(clickedElement).closest('tr').attr('data-project-uuid');
                console.log('details : ' + projectUuid);
                $.ajax({
                    url: "/api/protected/getProjectInfoForEditor",
                    data: {projectUuid: projectUuid},
                    method: 'GET',
                    dataType: "json",
                    timeout: 30000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var $infoContainer = $("div#project-info-container");
                        var $modal = $("#modalProjectInfo");
                        var template = $('#apkreverse-project-info-template').html();
                        var dataTemplate = {
                            response: data,
                            date_created_ago: jQuery.timeago(Date.parse(data.dateCreatedFormatted))
                        };
                        var html = Mustache.to_html(template, dataTemplate);
                        $infoContainer.empty();
                        $infoContainer.html(html);
                        $modal.removeData('bs.modal');
                        $modal.modal('show');
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            deleteProject: function (e, clickedElement) {
                e.preventDefault();
                var projectUuid = $(clickedElement).closest('tr').attr('data-project-uuid');
                var projectName = $(clickedElement).closest('tr').attr('data-project-name');
                //console.log('delete project : ' + projectUuid);
                w2confirm('Delete project: "' + projectName + '" ?')
                    .yes(function () {
                        // ajax delete project
                        $.ajax({
                            url: "/api/protected/deleteProject",
                            data: {projectUuid: projectUuid},
                            type: 'POST',
                            timeout: 15000,

                            beforeSend: function () {
                                // freeze UI until project data is loaded
                                $.blockUI({
                                    message: "Deleting project files. Please wait...",
                                    css: {
                                        border: 'none',
                                        padding: '15px',
                                        backgroundColor: '#000',
                                        '-webkit-border-radius': '10px',
                                        '-moz-border-radius': '10px',
                                        opacity: .5,
                                        color: '#fff'
                                    } });
                            },
                            success: function (data) {
                                // reload projects list
                                projectsListWrapper.reloadUiProjectsList();
                                toastr.success('Project is being removed in a background task!', 'Delete Project');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Removing project in background task");
                                }, 700);
                            },
                            error: function (xhr) {
                                var msg;
                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                    msg = xhr.statusText;
                                } else {
                                    msg = xhr.statusText + ": " + xhr.responseText;
                                }
                                notify(msg, "error", 7000);
                            },
                            complete: function () {
                                $.unblockUI();
                            }
                        });
                    })
                    .no(function () {
                    });
            }
        };

        // Debugger
        var appDebugger = {
            debHeight: null,
            debuggerObserver: null,
            isStartDebuggingProcessSubmitted: false,
            alwaysScrollToEnd: true,
            destroy: function () {
                if (appDebugger.debuggerObserver !== null) {
                    appDebugger.debuggerObserver.disconnect();
                    appDebugger.debuggerObserver = null;
                }
                w2ui['toolbar_debugger'].destroy();
                w2ui['layout_debugger'].destroy();
            },
            init: function () {
                // init the w2ui toolbar
                $('#toolbar_debugger').w2toolbar({
                    name: 'toolbar_debugger',
                    items: [
                        { type: 'button', id: 'toolbar-deb-run-logcat', img: 'icon-run', hint: 'Run logcat',
                            onClick: function (event) {
                                appDebugger.runDebugger();
                            }
                        },
                        { type: 'button', id: 'toolbar-deb-stop-logcat', img: 'icon-stop', hint: 'Stop logcat',
                            onClick: function (event) {
                                appDebugger.stopDebugger();
                            }
                        },
                        { type: 'break' },
                        { type: 'html', id: 'toolbar-deb-filter',
                            html: '<div style="padding: 0 10px;">' +
                                '<input id="row-filter-debugger" size="20" title="filter debugger output" maxlength="40" placeholder="type text to filter output" style="height: 25px;width: 190px;padding: 5px 25px 5px 5px; border-radius: 5px; border: 1px solid silver;color: #333;" autocomplete="off"/>' +
                                '</div>'
                        },
                        //{ type: 'break' },
                        { type: 'menu-radio', id: 'toolbar-deb-loglevel', img: 'icon-filter',
                            text: function (item) {
                                //var text = item.selected;
                                var el = this.get('toolbar-deb-loglevel:' + item.selected);
                                return 'Log level: ' + el.text;
                            },
                            selected: 'id-V',
                            items: [
                                { id: 'id-V', text: 'Verbose'},
                                { id: 'id-D', text: 'Debug'},
                                { id: 'id-I', text: 'Info'},
                                { id: 'id-W', text: 'Warning'},
                                { id: 'id-E', text: 'Error'},
                                { id: 'id-F', text: 'Fatal'}
                            ]
                        },
                        { type: 'break' },
                        { type: 'button', id: 'toolbar-deb-clear', img: 'icon-trash', hint: 'clear all',
                            onClick: function (event) {
                                appDebugger.clearOutput();
                            }
                        },
                        { type: 'check', id: 'toolbar-deb-scroll', img: 'icon-scroll-down', hint: 'always scroll to the end', checked: true,
                            onClick: function (event) {
                                //appDebugger.scrollToEnd();
                                var _this = w2ui['toolbar_debugger']['items'][7];
                                if (_this.checked === true) {
                                    appDebugger.alwaysScrollToEnd = false;
                                } else {
                                    appDebugger.alwaysScrollToEnd = true;
                                    $('#debugger-content-wrapper').scrollTop($('#debugger-lines').height());
                                }
                            }
                        },
                        { type: 'break' },
                        { type: 'button', id: 'toolbar-deb-print', img: 'icon-print', hint: 'print logs',
                            onClick: function (event) {
                                appDebugger.printOutput();
                            }
                        },
                        { type: 'break' },
                        { type: 'spacer' },
                        { type: 'check', id: 'toolbar-deb-min-max', img: 'icon-minmax', hint: 'minimize/maximize debugger',
                            onClick: function (event) {
                                var _this = w2ui['toolbar_debugger']['items'][12];
                                if (_this.checked === true) {
                                    $('#apktools-debugger-data').removeClass('maximized-div');
                                    if (appDebugger.debHeight) {
                                        $('#layout_debugger').height(appDebugger.debHeight);
                                    } else {
                                        $('#layout_debugger').height(410);
                                    }
                                } else {
                                    var $layout_debugger = $('#layout_debugger');
                                    appDebugger.debHeight = $layout_debugger.height();
                                    $('#apktools-debugger-data').addClass('maximized-div');
                                    $layout_debugger.height($(window).height() * 75 / 100);
                                }
                                w2ui['toolbar_debugger'].resize();
                                w2ui['layout_debugger'].resize();
                            }
                        }
                    ],


                    onClick: function (event) {
                        // event.taget = id of the clicked item
                        console.log('toolbar debugger item ' + event.target + ' is clicked.');
                        switch (event.target) {
                            case'toolbar-deb-loglevel:id-V':
                            case'toolbar-deb-loglevel:id-D':
                            case'toolbar-deb-loglevel:id-I':
                            case'toolbar-deb-loglevel:id-W':
                            case'toolbar-deb-loglevel:id-E':
                            case'toolbar-deb-loglevel:id-F':
                                var $debCont = $('#apkreverse-debugger-container');
                                var pid = $debCont.attr('data-proc-pid');
                                var name = $debCont.attr('data-proc-name');
                                var logLvlSelected = event.target.split(':')[1];

                                console.log('pid:' + pid + ' name:' + name + ' logLvlSelected:' + logLvlSelected);
                                if (pid && name) {
                                    appDebugger.startDebuggingProcess(pid, name, logLvlSelected);
                                }
                                break;
                        }
                    }
                });


                // js-tree search plugin init
                var callback_deb_filter = debounce(function (input, e) { // Js Tree filter search debounce ==> delay filtering
                    var code = e.keyCode || e.which;
                    var $rows = $("#debugger-lines > div");

                    if (code == '27') { // escape
                        $(input).val('');
                        $rows.show();
                    } else { // enter
                        var val = $.trim($(input).val()).replace(/ +/g, ' ').toLowerCase();
                        if (val === ' ' || val === '') {
                            $rows.show();
                            return;
                        }

                        $rows.show().filter(function () {
                            var text = $(this).text().replace(/\s+/g, ' ').toLowerCase();
                            return !~text.indexOf(val);
                        }).hide();
                    }
                }, 500);

                // listen to changes in the filter input
                $(document).on("keyup", "#row-filter-debugger", function (e) {
                    callback_deb_filter(this, e);
                });


                // init the w2ui layout
                var pstyleDebugger = 'border: 1px solid #dfdfdf; padding: 5px;';
                $('#layout_debugger').w2layout({
                    name: 'layout_debugger',
                    panels: [
                        { type: 'main', size: '100%', style: pstyleDebugger}
                    ]
                });

                w2ui['layout_debugger'].content('main', '<div style="background: #141414;position: absolute;top: 5px;bottom: 5px;left: 0;right: 5px;">' +
                    '<div id="debugger-content-wrapper" style="list-style-type: none;padding: 10px;height: 100%;overflow: auto;margin-bottom: 0 !important;">' +
                    '<div id="debugger-lines" style="width: 100%;color: #e1e1e1;font-family: monospace;font-size: 12px;"></div>' +
                    '</div>' +
                    '</div>');

                // observe the height of the fileEditor parent and resize w2ui layout when its height has changed
                if (appDebugger.debuggerObserver === null) {
                    var deb = document.querySelector('#layout_debugger');
                    var oldHeight = deb.style.height;
                    appDebugger.debuggerObserver = new MutationObserver(function (mutations) {
                        mutations.forEach(function (mutation) {
                            if (mutation.target === deb && mutation.attributeName === 'style' &&
                                oldHeight !== deb.style.height) {
                                oldHeight = deb.style.height;
                                w2ui['layout_debugger'].resize();
                            }
                        });
                    });
                    appDebugger.debuggerObserver.observe(deb, {attributes: true});
                }
            },
            runDebugger: function () {
                // if (device found) => ajax get list or running processes => w2popup select process; else error essage USB device not found
                // on select process => start logcat Thread and stream output to debugger
                // ajax instant run
                $.ajax({
                    url: "/api/protected/debuggerHandler",
                    data: {action: "GET_ALL_PROCESSES"},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        // freeze UI until response
                        $.blockUI({
                            message: "Getting list of processes<br>Please wait...",
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .5,
                                color: '#fff'
                            } });
                    },

                    success: function (data, textStatus, xhr) {
                        switch (xhr.status) {
                            case 204:
                                notify("Please wait while installing ADB...", "information", 5000);
                                break;
                            case 200:
                                // populate template and show w2popup to select PID
                                var template = $('#apkreverse-w2popup-debugger-select-proc-template').html();
                                var dataTemplate = {
                                    listProcesses: data.listProcesses
                                };
                                var html = Mustache.to_html(template, dataTemplate);

                                w2popup.open({
                                    title: 'Select process',
                                    body: html,
                                    buttons: '<button class="w2ui-btn" onclick="ApkToolModule.onDebuggerProcessSelected();">Debug Process</button>' +
                                        '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                                    width: 450,
                                    height: 320,
                                    overflow: 'hidden',
                                    color: '#000',
                                    speed: '0.3',
                                    opacity: '0.4',
                                    modal: true,
                                    showClose: true,
                                    //showMax: true,
                                    onOpen: function (event) {
                                        //console.log('open');
                                    },
                                    onClose: function (event) {
                                        //console.log('close');
                                    },
                                    onKeydown: function (event) {
                                    }
                                });
                                break;
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    }, complete: function () {
                        $.unblockUI();
                    }
                });

            },
            onDebuggerProcessSelected: function () {
                var $procSelect = $("#input-deb-running-procs");
                var pid = $procSelect.val();
                var name = $procSelect.find("option:selected").text();
                var logLvlSelected = w2ui['toolbar_debugger']['items'][4].selected;
                appDebugger.startDebuggingProcess(pid, name, logLvlSelected);
            },
            startDebuggingProcess: function (pid, name, logLvlSelected) {
                if (!pid) {
                    notify("Please select a process!", "error", 5000);
                    return;
                }

                var loglvl;
                switch (logLvlSelected) {
                    case "id-V":
                        loglvl = "V";
                        break;
                    case "id-D":
                        loglvl = "D";
                        break;
                    case "id-I":
                        loglvl = "I";
                        break;
                    case "id-W":
                        loglvl = "W";
                        break;
                    case "id-E":
                        loglvl = "E";
                        break;
                    case "id-F":
                        loglvl = "F";
                        break;
                    default :
                        loglvl = "V";
                        break;
                }

                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (appDebugger.isStartDebuggingProcessSubmitted === false) {
                    appDebugger.isStartDebuggingProcessSubmitted = true;
                } else {
                    notify("The Debugger is already running, please wait!", "warning", 5000);
                    return;
                }
                // make an ajax call to start the process of debugging
                $.ajax({
                    url: "/api/protected/debuggerHandler",
                    data: {action: "START_DEBUGGING", pid: pid, loglvl: loglvl},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        $('#debugger-lines').empty();
                        var $debCont = $('#apkreverse-debugger-container');
                        $debCont.attr('data-proc-pid', pid);
                        $debCont.attr('data-proc-name', name);
                        $('#deb-selected-proc-name').text("(" + name + ")");
                        w2popup.close();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        appDebugger.isStartDebuggingProcessSubmitted = false;
                    }
                });
            },
            stopDebugger: function () {
                // make an ajax call to start the process of debugging
                $.ajax({
                    url: "/api/protected/debuggerHandler",
                    data: {action: "STOP_DEBUGGING"},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        // freeze UI until response
                        $.blockUI({
                            message: "Stopping debbuger and clearing log buffer.<br>Please wait...",
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .5,
                                color: '#fff'
                            } });
                    },
                    success: function (data) {
                        $('#debugger-lines').empty();
                        $('#deb-selected-proc-name').text('');
                        var $debCont = $('#apkreverse-debugger-container');
                        $debCont.attr('data-proc-pid', '');
                        $debCont.attr('data-proc-name', '');
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        $.unblockUI();
                    }
                });
            },
            debuggerInputInfo: function (textReceived) { // when text is received from adb through web scket we check filter
                var logClass;
                var logLevel = textReceived.substring(19, 20);
                switch (logLevel) {
                    case "V":
                        logClass = "llv";
                        break;
                    case "D":
                        logClass = "lld";
                        break;
                    case "I":
                        logClass = "lli";
                        break;
                    case "W":
                        logClass = "llw";
                        break;
                    case "E":
                        logClass = "lle";
                        break;
                    case "F":
                        logClass = "llf";
                        break;
                    case "S":
                        logClass = "lls";
                        break;
                    default:
                        logClass = "";
                        break;
                }

                var canShow;
                var val = $.trim($('#row-filter-debugger').val()).replace(/ +/g, ' ').toLowerCase();
                if (val === ' ' || val === '') {
                    canShow = true;
                } else {
                    canShow = ~textReceived.replace(/\s+/g, ' ').toLowerCase().indexOf(val);
                }
                return {logClass: logClass, canShow: canShow};
            },
            clearOutput: function () {
                $('#debugger-lines').empty();
            },
            printOutput: function () {
                var doc = new jsPDF('p', 'pt', 'a4');
                doc.fromHTML($('#debugger-lines').html(), 0, 0, {
                    'width': 500
                });
                doc.save('apk-debugger.pdf');
            }
        };


        // project editor wrapper
        var projectEditor = {
            feHeight: null,
            feObserver: null,
            codeEditors: {},
            cmMarkers: [],
            isPackageNameChangerSubmitted: false,
            isPackageRenamerSubmitted: false,
            isAppInfoUpdaterSubmitted: false,
            isManifestEntriesRenamerSubmitted: false,
            isAppNameModifierSubmitted: false,
            isBuildDebugApkSubmitted: false,
            isBuildReleaseApkSubmitted: false,
            destroy: function () {
                $(window).off('beforeunload');
                $('#tab-apkreverse-project-editor').removeClass('maximized-div');
                $("#jstree-tree-fileeditor").jstree('destroy');
                $(".fileeditor-search-tree").jstree('destroy'); // destroy all search results jsTree
                if (projectEditor.feObserver !== null) {
                    projectEditor.feObserver.disconnect();
                    projectEditor.feObserver = null;
                }
                w2ui['toolbar_file_editor'].destroy();
                w2ui['tabs_file_editor'].destroy();
                w2ui['layout_file_editor'].destroy();
                w2ui['bottom_file_editor'].destroy();
                $("div#tab-apkreverse-project-editor").html('');
                // delete all code editors references
                for (var prop in projectEditor.codeEditors) {
                    if (projectEditor.codeEditors.hasOwnProperty(prop)) {
                        console.log("deleting editor : " + prop);
                        delete projectEditor.codeEditors[prop];
                    }
                }
                projectEditor.cmMarkers.length = 0; // clear all markers
                projectEditor.folderExplorer.resetStateOnCloseProject();
            },
            init: function (project) {
                $(window).on('beforeunload', function () {
                    // if project editor is opened we must prompt the user before leaving the page
                    return 'Are you sure you want close this project?';
                });
                //console.log("project data ====>" + JSON.stringify(project));
                var template = $('#apkreverse-project-editor-template').html();
                var dataTemplate = {
                    project: project
                };
                var html = Mustache.to_html(template, dataTemplate);
                $("div#tab-apkreverse-project-editor").html(html);

                // init the w2ui toolbar
                $('#toolbar_file_editor').w2toolbar({
                    name: 'toolbar_file_editor',
                    items: [
                        { type: 'html', id: 'toolbar-btn-project-search',
                            html: '<div style="padding: 0px 10px;">' +
                                '<input id="file-filter-editor" size="20" title="filter project files" maxlength="40" placeholder="type text and press enter..." style="height: 25px;width: 190px;padding: 5px 25px 5px 5px; border-radius: 5px; border: 1px solid silver;color: #333;" autocomplete="off"/>' +
                                '<img id="jstree-search-img-loader" src="/static/public/images/util/loading.gif" style="margin-left: -21px;margin-top: -4px;display: none;" width="17" height="17">' +
                                '<img id="jstree-search-img-clear" src="/static/public/images/util/clear-search.png" style="margin-left: -21px;margin-top: -4px;display: none;cursor: pointer;" width="17" height="17">' +
                                '</div>'
                        },
                        { type: 'break' },
                        { type: 'button', id: 'toolbar-btn-project-info', caption: 'Project info', img: 'icon-toolinfo', hint: 'show project details',
                            onClick: function (event) {
                                projectEditor.getProjectInfo();
                            }
                        },
                        { type: 'break'},
                        { type: 'button', id: 'toolbar-btn-folder-explorer', caption: 'Explorer', img: 'icon-toolexplorer', hint: 'explore project files',
                            onClick: function (event) {
                                projectEditor.folderExplorer.openToolbarExplorer();
                            }
                        },
                        { type: 'break'},
                        { type: 'menu', id: 'toolbar-menu-search', caption: 'Text search', img: 'icon-toolsearch', hint: 'search text inside project files',
                            items: [
                                { id: 'tb-find', text: 'Find'},
                                { id: 'tb-find-and-replace', text: 'Find and replace'}
                            ]},
                        { type: 'break' },
                        { type: 'menu', id: 'toolbar-menu-tools', caption: 'Tools', img: 'icon-tooltools', hint: 'change package name, update app version,...',
                            items: [
                                { id: 'tb-package-changer', text: 'Package Name Changer'},
                                { id: 'tb-info-updater', text: 'Version/SDK Updater'},
                                { id: 'tb-package-renamer', text: 'Any Package Renamer'},
                                { id: 'tb-manifest-renamer', text: 'Manifest Entries Transformer'},
                                { text: '--' },
                                { id: 'tb-icon-modifier', text: 'App Icon Modifier'},
                                { id: 'tb-appname-modifier', text: 'App Name Modifier'}
                            ]},
                        { type: 'break' },
                        { type: 'menu', id: 'toolbar-menu-inj', caption: 'Injector', img: 'icon-syringe', hint: 'Inject utilities...',
                            items: [
                                { id: 'tb-app-anti-reverse', text: 'Anti Reverse Kit'},
                                { id: 'tb-admob-sched-bug', text: 'Scheduled Bug'},
                                { id: 'tb-admob-inj', text: 'Admob IDs injector'},
                                { id: 'tb-admob-consent', text: 'Admob Consent injector'}
                            ]},
                        { type: 'break'},
                        { type: 'menu', id: 'toolbar-menu-apk', caption: 'Build apk', img: 'icon-toolbuild', hint: 'generate apk file for debug/release',
                            items: [
                                { id: 'tb-apk-debug', text: 'Build DEBUG apk'},
                                { id: 'tb-apk-release', text: 'Build RELEASE apk'}
                            ]},
                        { type: 'break' },
                        { type: 'button', id: 'toolbar-btn-instant-run', img: 'icon-run', hint: 'instant run (build and run on connected USB device)',
                            onClick: function (event) {
                                w2confirm('Make sure you have a connected Android device to USB port!<br>' +
                                    '<span style="font-size: 9px">Instant run will build a DEBUG apk and launch it on USB connected device.</span><br>Continue with INSTANT RUN?')
                                    .yes(function () {
                                        // ajax instant run
                                        $.ajax({
                                            url: "/api/protected/toolsOfEditor",
                                            data: {action: "INSTANT_RUN", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                            method: 'POST',
                                            dataType: "json",
                                            timeout: 15000,
                                            cache: false,
                                            beforeSend: function () {
                                                showBusysign();
                                            },
                                            success: function (data) {
                                                // show editor bottom tab 'Logs'
                                                try {
                                                    if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                                        w2ui['layout_file_editor'].show('bottom', window.instant);
                                                    }
                                                    w2ui['bottom_file_editor'].click('feBottomTabLog');
                                                } catch (err) {
                                                    // do nothing
                                                }
                                            },
                                            error: function (xhr) {
                                                var msg;
                                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                    msg = xhr.statusText;
                                                } else {
                                                    msg = xhr.statusText + ": " + xhr.responseText;
                                                }
                                                notify(msg, "error", 7000);
                                            }, complete: function () {
                                                hideBusysign();
                                            }
                                        });
                                    })
                                    .no(function () {
                                    });
                            }
                        },
                        { type: 'break'},
                        { type: 'spacer' },
                        { type: 'check', id: 'toolbar-toggle-bottom-layout', checked: true, img: 'icon-bottomly', hint: 'show/hide bottom layout',
                            onClick: function (event) {
                                w2ui['layout_file_editor'].toggle('bottom', window.instant);
                            }
                        },
                        { type: 'break'},
                        { type: 'check', id: 'toolbar-min-max-editor', img: 'icon-minmax', hint: 'minimize/maximize editor',
                            onClick: function (event) {
                                var _this = w2ui['toolbar_file_editor']['items'][19];
                                if (_this.checked === true) {
                                    $('#tab-apkreverse-project-editor').removeClass('maximized-div');
                                    if (projectEditor.feHeight) {
                                        $('#layout_file_editor').height(projectEditor.feHeight);
                                    } else {
                                        $('#layout_file_editor').height(410);
                                    }
                                } else {
                                    var $layout_file_editor = $('#layout_file_editor');
                                    projectEditor.feHeight = $layout_file_editor.height();
                                    $('#tab-apkreverse-project-editor').addClass('maximized-div');
                                    $layout_file_editor.height($(window).height() * 85 / 100);
                                }
                                w2ui['toolbar_file_editor'].resize();
                                w2ui['layout_file_editor'].resize();
                            }
                        }
                    ],
                    onClick: function (event) {
                        // event.taget = id of the clicked item
                        //console.log('toolbar item ' + event.target + ' is clicked.');
                        switch (event.target) {
                            case'toolbar-menu-search:tb-find':
                                projectEditor.textSearchClicked();
                                break;
                            case'toolbar-menu-search:tb-find-and-replace':
                                projectEditor.textSearchAndReplaceClicked();
                                break;
                            case'toolbar-menu-tools:tb-icon-modifier':
                                projectEditor.toolsAppIconModifierClicked();
                                break;
                            case'toolbar-menu-tools:tb-appname-modifier':
                                projectEditor.toolsAppNameModifierClicked();
                                break;
                            case'toolbar-menu-tools:tb-package-changer':
                                projectEditor.toolsPackageNameChangerClicked();
                                break;
                            case'toolbar-menu-tools:tb-info-updater':
                                projectEditor.toolsAppInfoUpdaterClicked();
                                break;
                            case 'toolbar-menu-tools:tb-package-renamer':
                                projectEditor.toolsPackageRenamerClicked();
                                break;
                            case 'toolbar-menu-tools:tb-manifest-renamer':
                                projectEditor.toolsManifestEntriesRenamerClicked();
                                break;
                            case 'toolbar-menu-apk:tb-apk-debug':
                                projectEditor.toolsBuildApkForDebugClicked();
                                break;
                            case 'toolbar-menu-apk:tb-apk-release':
                                projectEditor.toolsBuildApkForReleaseClicked();
                                break;
                        }
                    }
                });


                // init the w2ui layout
                var pstyleFE = 'border: 1px solid #dfdfdf; padding: 5px;';
                $('#layout_file_editor').w2layout({
                    name: 'layout_file_editor',
                    panels: [
                        { type: 'left', size: 330, style: pstyleFE, resizable: true },
                        { type: 'main', style: pstyleFE},
                        { type: 'bottom', size: '34%', resizable: true, style: pstyleFE, content: 'bottom' }
                    ]
                });
                // project files tree in the editor left panel
                w2ui['layout_file_editor'].content('left', '<div id="jstree-tree-fileeditor" style="margin-top: 5px;"></div>');

                // populate apk files template for 'feBottomTabApkFiles-content'
                var templateApkFilesInfo = $('#apkreverse-project-apkfiles-info-template').html();
                var dataTemplateApkFilesInfo = {
                    project: project
                };
                var htmlApkFilesInfo = Mustache.to_html(templateApkFilesInfo, dataTemplateApkFilesInfo);
                // editor bottom panel tabs
                w2ui['layout_file_editor'].content('bottom', '<div id="bottom_file_editor" style="width: 100%;"></div>' +
                    '<div style="background: #191927;position: absolute;top: 34px;bottom: 5px;left: 0;right: 5px;">' +
                    '<ul id="fe-bottom-list-tab-content" style="list-style-type: none;padding: 0;height: 100%;overflow: auto;margin-bottom: 0 !important;">' +
                    '<li id="feBottomTabLog-li" class="" style="color: #0cc500;font-family: monospace;font-size: 12px;"><div><div title="clear logs" class="w2ui-icon icon-trash clear-bottom-log-btn" onclick="$(\'#feBottomTabLog-content\').empty();"></div><button class="btn btn-danger btn-xm glyphicon glyphicon-stop stop-process-btn" title="cancel running process (stop it)" id="cancel-log-process" data-process-id="" data-process-type="" onclick="ApkToolModule.cancelUserProcess(this);"></button></div><div id="feBottomTabLog-content" style="padding: 10px;"></div></li>' +
                    '<li id="feBottomTabSearch-li" class="hidden" style="color: #00adc2;font-family: monospace;font-size: 12px;"><div><div title="clear text search" class="w2ui-icon icon-trash clear-bottom-log-btn" onclick="$(\'#feBottomTabSearch-content\').empty();"></div><button class="btn btn-danger btn-xm glyphicon glyphicon-stop stop-process-btn" title="cancel running process (stop it)" id="cancel-text-process" data-process-id="" data-process-type="" onclick="ApkToolModule.cancelUserProcess(this);"></button></div><div id="feBottomTabSearch-content" style="padding: 10px;"></div></li>' +
                    '<li id="feBottomTabApkFiles-li" class="hidden" style="color: #c9bb08;font-family: monospace;font-size: 12px;"><div id="feBottomTabApkFiles-content" style="padding: 10px;">' + htmlApkFilesInfo + '</div></li>' +
                    '</ul>' +
                    '</div>');
                $('#bottom_file_editor').w2tabs({
                    name: 'bottom_file_editor',
                    active: 'feBottomTabLog',
                    tabs: [
                        { id: 'feBottomTabLog', caption: 'Logs' },
                        { id: 'feBottomTabSearch', caption: 'Text search' },
                        { id: 'feBottomTabApkFiles', caption: 'Apk files' }
                    ],
                    onClick: function (event) { // hide other tabs content and show only selected one
                        //console.log("selected tab: " + event.target);

                        var selected_li_id = event.target + '-li'; // id of the selected tab content liste items

                        // hide other tabs and save their scroll position
                        var $elements_to_hide = $("ul#fe-bottom-list-tab-content > li:not(#" + selected_li_id + ")");
                        $elements_to_hide.each(function (idx, li) {
                            if (!$(li).hasClass('hidden')) {
                                var div_content_id = $(li).attr('id').replace('-li', '-content');
                                //console.log('div_content_id to hide : ' + div_content_id);
                                var $div_content = $('#' + div_content_id);
                                var scroll_position = $('ul#fe-bottom-list-tab-content').scrollTop();
                                $div_content.attr('data-scroll', scroll_position);
                            }
                            $(li).removeClass('hidden').addClass('hidden');
                        });

                        // show selected tab and restore its scroll position
                        var $element_to_show = $("ul#fe-bottom-list-tab-content > li#" + selected_li_id);
                        $element_to_show.removeClass('hidden');
                        var $div_content = $('#' + selected_li_id.replace('-li', '-content'));
                        var scroll_position = $div_content.attr('data-scroll');
                        $('ul#fe-bottom-list-tab-content').scrollTop(scroll_position);
                    }
                });

                w2ui['layout_file_editor'].content('main', '<div id="tabs_file_editor" style="width: 100%;"></div>' +
                    '<div style="padding: 10px;position: absolute;right: 0;left: 0;bottom: 0px;top: 30px;">' +
                    '<ul id="fe-main-list-tab-content" class="" style="list-style-type: none;padding: 0;height: 100%;">' +
                    '</ul>' +
                    '</div>');

                $('#tabs_file_editor').w2tabs({
                    name: 'tabs_file_editor',
                    tabs: [],
                    onClick: function (event) {
                        projectEditor.tabsManager.onTabClick(event);
                    },
                    onClose: function (event) {
                        projectEditor.tabsManager.onTabClose(event);
                    },
                    onRender: function () {
                        $('#tabs_file_editor').hide();
                    }
                });


                // js-tree init
                $('#jstree-tree-fileeditor')
                    .jstree({
                        plugins: ['themes', 'types', 'wholerow', 'search', 'contextmenu', 'dnd', 'sort', 'massload'],
                        core: {
                            check_callback: function (operation, node, node_parent, node_position, more) {
                                // operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
                                // in case of 'rename_node' node_position is filled with the new node name
                                if (operation === "move_node") {
                                    return node_parent.type === "folder"; //only allow dropping inside nodes of type 'folder'
                                }
                                return true;  //allow all other operations
                            },
                            themes: {
                                name: "default-dark",
                                dots: true,
                                icons: true
                            },
                            data: {
                                timeout: 30000,
                                cache: false,
                                url: function (node) {
                                    return '/api/protected/getNodeInfoForEditor';
                                },
                                data: function (node) {
                                    return { 'id': node.id, projectUuid: project.projectUuid};
                                }
                            }
                        },
                        massload: {
                            url: "/api/protected/massloadNodeForEditor",
                            data: function (nodes) {
                                return { "ids": nodes.join(","), projectUuid: project.projectUuid};
                            },
                            timeout: 25000
                        },
                        search: {
                            case_insensitive: true,
                            show_only_matches: true,
                            ajax: {
                                url: "/api/protected/searchNodeForEditor",
                                data: {
                                    projectUuid: project.projectUuid
                                },
                                dataType: 'json',
                                type: 'GET',
                                timeout: 25000
                            }
                        },
                        dnd: {
                            drop_target: false,
                            drag_target: false
                        },
                        types: {
                            folder: {
                                icon: '/static/public/plugins/jstree/b_folder.png'
                            },

                            edit_txt: {
                                icon: '/static/public/plugins/jstree/a_edit_txt.png'
                            },
                            edit_smali: {
                                icon: '/static/public/plugins/jstree/a_edit_smali.png'
                            },
                            edit_xml: {
                                icon: '/static/public/plugins/jstree/a_edit_xml.png'
                            },
                            edit_json: {
                                icon: '/static/public/plugins/jstree/a_edit_json.png'
                            },
                            edit_html: {
                                icon: '/static/public/plugins/jstree/a_edit_html.png'
                            },
                            edit_js: {
                                icon: '/static/public/plugins/jstree/a_edit_js.png'
                            },
                            edit_css: {
                                icon: '/static/public/plugins/jstree/a_edit_css.png'
                            },
                            load_pdf: {
                                icon: '/static/public/plugins/jstree/a_load_pdf.png'
                            },
                            load_img: {
                                icon: '/static/public/plugins/jstree/a_load_img.png'
                            },
                            load_video: {
                                icon: '/static/public/plugins/jstree/a_load_video.png'
                            },
                            load_audio: {
                                icon: '/static/public/plugins/jstree/a_load_audio.png'
                            },
                            download: {
                                icon: '/static/public/plugins/jstree/a_download.png'
                            }
                        },
                        sort: function (a, b) { // sort by icon and after by text
                            var a1 = this.get_node(a);
                            var b1 = this.get_node(b);
                            if (a1.icon == b1.icon) {
                                return (a1.text > b1.text) ? 1 : -1;
                            } else {
                                return (a1.icon > b1.icon) ? 1 : -1;
                            }
                        },
                        contextmenu: {
                            items: function (node) {
                                // The default set of all items
                                var items = {
                                    createFolderItem: {
                                        label: "create new folder",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.createFolderItem(node);

                                        }
                                    },
                                    openItem: {
                                        label: "open",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.openItem(node);
                                        }
                                    },
                                    renameItem: { // The "rename" menu item
                                        label: "rename",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.renameItem(node);
                                        }
                                    },
                                    replaceItem: { // The "delete" menu item
                                        label: "replace by...",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.replaceItem(node);
                                        }
                                    },
                                    deleteItem: { // The "delete" menu item
                                        label: "delete",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.deleteItem(node);
                                        }
                                    },
                                    addFilesItem: { // The "rename" menu item
                                        label: "add files",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.addFilesItem(node);
                                        }
                                    },
                                    downloadItem: { // The "rename" menu item
                                        label: "download",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.downloadItem(node);
                                        }
                                    },
                                    showInExplorerItem: {
                                        label: "show in explorer",
                                        action: function (data) {
                                            var inst = $.jstree.reference(data.reference);
                                            var node = inst.get_node(data.reference);
                                            projectEditor.contextMenuWrapper.showInExplorerItem(node.id);
                                        }
                                    }
                                };

                                if (node.type === 'folder') {
                                    delete items.openItem;
                                    delete items.replaceItem;
                                    delete items.downloadItem;
                                    // do not rename or delete project's root folder
                                    if (node.id === '0') {
                                        delete items.renameItem;
                                        delete items.deleteItem;
                                    }
                                } else {
                                    delete items.addFilesItem;
                                    delete items.createFolderItem;
                                    delete items.showInExplorerItem;
                                }
                                return items;
                            },
                            select_node: true
                        }
                    }).bind("select_node.jstree", function (event, data) {
                        // if a node is clicked check if its tab is opened => if so set it as selected
                        if (data.node.type !== 'folder') {
                            console.log("selected node_id: " + data.node.id);
                            if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                for (var p = 0; p < w2ui['tabs_file_editor'].tabs.length; p++) {
                                    var openedNodeId = w2ui['tabs_file_editor'].tabs[p].id;
                                    if (String(openedNodeId) === String(data.node.id)) { // its tab is opened => set it as active
                                        // hide other tabs content and show only selected one
                                        $("ul#fe-main-list-tab-content li:not([data-node-id='" + openedNodeId + "'])").removeClass('hidden').addClass('hidden');
                                        $("ul#fe-main-list-tab-content li[data-node-id='" + openedNodeId + "']").removeClass('hidden');
                                        w2ui['tabs_file_editor'].select(openedNodeId);
                                        break;
                                    }
                                }
                            }
                        }
                    }).bind("dblclick.jstree", function (event) {
                        // on double click on file => open it in a tab
                        var tree = $(this).jstree();
                        var node = tree.get_node(event.target);

                        if (node.type !== 'folder') {
                            // open file it in a tab
                            projectEditor.tabsManager.openTab(node, project.projectUuid)
                        }
                    }).bind("ready.jstree", function (event) {
                        // js-tree search plugin init
                        var callback_search = debounce(function (input, e) { // Js Tree filter search debounce ==> delay filtering
                            var code = e.keyCode || e.which;
                            var tree = $('#jstree-tree-fileeditor');

                            if ($(input).val() === '') {
                                tree.jstree(true).show_all();
                                $('#jstree-search-img-clear').hide();
                            }
                            if (code == '27') { // escape
                                //tree.jstree(true).clear_search();
                                $(input).val('');
                                tree.jstree(true).show_all();
                                $('#jstree-search-img-clear').hide();
                            } else if (code == '13') { // enter
                                if ($(input).val().trim() === '') {
                                    notify("Please enter a keyword, then submit!", "warning", 5000)
                                    return;
                                }
                                //tree.jstree(true).clear_search();
                                console.log('searching for : ' + $(input).val());
                                $('#jstree-search-img-loader').show();
                                $('#jstree-search-img-clear').hide();
                                //tree.jstree(true).show_all();
                                tree.jstree(true).search($(input).val().trim());
                                $("#file-filter-editor").prop('disabled', true);
                            }
                        }, 250);

                        // listen to changes in the search input, if user hit enter, then perform jstree search
                        $("#file-filter-editor").keyup(function (e) {
                            callback_search(this, e);
                        });
                        // bind click event to search-clear-image, to clear the search input and show the whole tree
                        $('#jstree-search-img-clear').on('click', function () {
                            var tree = $('#jstree-tree-fileeditor');
                            //tree.jstree(true).clear_search();
                            var $file_filer_input = $("#file-filter-editor");
                            $file_filer_input.val('');
                            tree.jstree(true).show_all();
                            $('#jstree-search-img-clear').hide();
                            $file_filer_input.focus();
                        });
                    }).bind('search.jstree', function (e, data) {
                        //console.log('jstree search found ' + data.nodes.length);
                        // bind on search results ready
                        $('#jstree-search-img-loader').hide();
                        $('#jstree-search-img-clear').show();
                        $("#file-filter-editor").prop('disabled', false);
                        /*if (data.nodes.length === 0) {
                         $('#jstree-tree-fileeditor').jstree(true).hide_all();
                         }*/
                        $('#file-filter-editor').w2tag('Search ended', { position: 'right' });
                        setTimeout(function () {
                            $('#file-filter-editor').w2tag('');
                        }, 1500);
                    });
                // observe the height of the fileEditor parent and resize w2ui layout when its height has changed
                if (projectEditor.feObserver === null) {
                    var fe = document.querySelector('#layout_file_editor');
                    var oldHeight = fe.style.height;
                    projectEditor.feObserver = new MutationObserver(function (mutations) {
                        mutations.forEach(function (mutation) {
                            if (mutation.target === fe && mutation.attributeName === 'style' &&
                                oldHeight !== fe.style.height) {
                                oldHeight = fe.style.height;
                                w2ui['layout_file_editor'].resize();
                            }
                        });
                    });
                    projectEditor.feObserver.observe(fe, {attributes: true});
                }
                // show project editor tab when UI initialization ended
                $('ul#projects-hidden-navtab a[href="#tab-apkreverse-project-editor"]').tab('show');

            },
            tabsManager: {
                openTab: function (node, projectUuid, callbackOnTabOpened) { // open file in a tab
                    var foundTab = false;
                    var listItems = $("ul#fe-main-list-tab-content > li");
                    listItems.each(function (idx, li) {
                        if ($(li).attr("data-node-id") === node.id) {
                            foundTab = true;
                            return false;
                        }
                    });

                    if (foundTab) { // tab already opened => set as active
                        w2ui['tabs_file_editor'].select(node.id);
                        console.log("tab already opened  => : id:" + node.id);
                        listItems.each(function (idx, li) {
                            if ($(li).attr("data-node-id") === node.id) {
                                $(li).removeClass('hidden');
                            } else {
                                $(li).removeClass('hidden').addClass('hidden');
                            }
                        });
                        if (callbackOnTabOpened) {
                            callbackOnTabOpened.call();
                        }
                    } else { // new tab => open and set as active
                        var getFileFromServer = function (node_, projectUuid_, callbackOnTabOpened_) {
                            // get file content from server
                            $.ajax({
                                url: "/api/protected/getFileContent",
                                data: {nodeId: node_.id, projectUuid: projectUuid_},
                                method: 'GET',
                                dataType: "json",
                                timeout: 30000,
                                cache: false,
                                beforeSend: function () {
                                    // freeze UI until project data is loaded
                                    $('#project-editor-devenvironment-data').block({
                                        message: "Opening file. Please wait...",
                                        centerY: false,
                                        centerX: false,
                                        css: {
                                            border: 'none',
                                            padding: '15px',
                                            backgroundColor: '#000',
                                            '-webkit-border-radius': '10px',
                                            '-moz-border-radius': '10px',
                                            opacity: .5,
                                            color: '#fff',
                                            position: 'absolute',
                                            margin: 'auto'
                                        }
                                    });
                                },
                                success: function (data) {
                                    // data contains the projectUuid
                                    projectEditor.fileWrapper.openNewFileInEditorTab(node_, data, callbackOnTabOpened_);
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                complete: function () {
                                    $('#project-editor-devenvironment-data').unblock();
                                }
                            });
                        };

                        // if unknown file w2confirm open in edit mode
                        if (node.type === 'download') {
                            w2confirm('This file is not supported by this editor!<br>' +
                                '<span style="font-size: 9px">If you want to modify it, you are strongly recommended ' +
                                'to download it, make modifications and then upload it!</span><br>Do want to open it as text file?')
                                .yes(function () {
                                    getFileFromServer(node, projectUuid, callbackOnTabOpened);
                                })
                                .no(function () {
                                });

                        } else {
                            getFileFromServer(node, projectUuid, callbackOnTabOpened);
                        }
                    }
                },
                openTabFromExplorer: function (nodeId, nodeType, projectUuid) { // open file in a tab
                    var foundTab = false;
                    var listItems = $("ul#fe-main-list-tab-content > li");
                    listItems.each(function (idx, li) {
                        if ($(li).attr("data-node-id") === nodeId) {
                            foundTab = true;
                            return false;
                        }
                    });

                    if (foundTab) { // tab already opened => set as active
                        w2ui['tabs_file_editor'].select(nodeId);
                        console.log("tab already opened  => : id:" + nodeId);
                        listItems.each(function (idx, li) {
                            if ($(li).attr("data-node-id") === nodeId) {
                                $(li).removeClass('hidden');
                            } else {
                                $(li).removeClass('hidden').addClass('hidden');
                            }
                        });
                        var tree = $('#jstree-tree-fileeditor');
                        // set corresponding jstree node as selected
                        tree.jstree("deselect_all");
                        tree.jstree('select_node', nodeId);
                        // bring focus to corresponding jstree node with a scroll animation
                        var nodeLine = tree.jstree(true).get_node(nodeId, true).children('.jstree-anchor');
                        var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                        if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                            nodeLine.focus();
                        } else { // if not not visible inside viewport => scroll with animation
                            container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                nodeLine.focus();
                            });
                        }
                    } else { // new tab => load file from server open and set as active
                        var getFileFromServer = function (nodeId_) {
                            // get file content from server
                            //ajax call LOAD_NODE_DATA
                            $.ajax({
                                url: "/api/protected/contextMenuForEditor",
                                data: {action: "LOAD_NODE_DATA", nodeId: nodeId_, projectUuid: projectUuid},
                                method: 'POST',
                                dataType: "json",
                                timeout: 20000,
                                cache: false,
                                success: function (data) {
                                    // jstree load nodes with callback
                                    // callback let us select the jsTree node and bring focus to it with  animation
                                    $('#jstree-tree-fileeditor').jstree(true).load_node(data.parentsArray, function () {
                                        var node = $('#jstree-tree-fileeditor').jstree(true).get_node(data.nodeId);
                                        projectEditor.tabsManager.openTab(node, $('#project-editor-devenvironment-data').attr('data-project-uuid'), function () {
                                            var tree = $('#jstree-tree-fileeditor');
                                            // set corresponding jstree node as selected
                                            tree.jstree("deselect_all");
                                            tree.jstree('select_node', node.id);
                                            // bring focus to corresponding jstree node with a scroll animation
                                            var nodeLine = tree.jstree(true).get_node(node.id, true).children('.jstree-anchor');
                                            var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                                            if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                                                nodeLine.focus();
                                            } else { // if not not visible inside viewport => scroll with animation
                                                container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                                    nodeLine.focus();
                                                });
                                            }
                                        });
                                    });
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                }
                            });
                        };
                        getFileFromServer(nodeId);
                    }
                },
                onTabClick: function (event) {
                    // hide other tabs content and show only selected one
                    $("ul#fe-main-list-tab-content li:not([data-node-id='" + event.target + "'])").removeClass('hidden').addClass('hidden');
                    $("ul#fe-main-list-tab-content li[data-node-id='" + event.target + "']").removeClass('hidden');
                    var tree = $('#jstree-tree-fileeditor');
                    // set corresponding jstree node as selected
                    tree.jstree("deselect_all");
                    tree.jstree('select_node', event.target);
                    // bring focus to corresponding jstree node with a scroll animation
                    var nodeLine = tree.jstree(true).get_node(event.target, true).children('.jstree-anchor');
                    var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                    if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                        nodeLine.focus();
                    } else { // if not not visible inside viewport => scroll with animation
                        container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                            nodeLine.focus();
                        });
                    }
                },
                onTabClose: function (event) {
                    console.log("close tab " + event.target);
                    var listItems = $("ul#fe-main-list-tab-content li");
                    listItems.each(function (idx, li) {
                        if ($(li).attr("data-node-id") === event.target) {
                            if (!$(li).hasClass('hidden')) {
                                var next_id = $(li).next('li').attr("data-node-id");
                                var prev_id = $(li).prev('li').attr("data-node-id");
                                if (next_id) {
                                    w2ui['tabs_file_editor'].click(next_id)
                                } else if (prev_id) {
                                    w2ui['tabs_file_editor'].click(prev_id)
                                }
                            }
                            $(li).remove();
                            return false;
                        }
                    });
                    if ($('ul#fe-main-list-tab-content li').length === 0) {
                        $('#tabs_file_editor').hide();
                    }
                    // remove the editor instance
                    // delete all code editors references
                    if (projectEditor.codeEditors.hasOwnProperty(event.target)) {
                        console.log("deleting editor onClose: " + event.target);
                        delete projectEditor.codeEditors[event.target];
                    }
                },
                editorTabsContextMenuClicked: function (tabId, action) {
                    if (w2ui['tabs_file_editor'].tabs.length == 0)
                        return;

                    var listItems = $("ul#fe-main-list-tab-content li");
                    switch (action) {
                        case "close":
                        {
                            console.log("close tab: " + tabId);
                            for (var j = 0; j < w2ui['tabs_file_editor'].tabs.length; j++) {
                                if (w2ui['tabs_file_editor'].tabs[j].id == tabId) {
                                    // remove content from content list
                                    listItems.each(function (idx, li) {
                                        if ($(li).attr("data-node-id") == tabId) {
                                            if (!$(li).hasClass('hidden')) {
                                                var next_id = $(li).next('li').attr("data-node-id");
                                                var prev_id = $(li).prev('li').attr("data-node-id");
                                                if (next_id) {
                                                    w2ui['tabs_file_editor'].click(next_id)
                                                } else if (prev_id) {
                                                    w2ui['tabs_file_editor'].click(prev_id)
                                                }
                                            }
                                            $(li).remove();
                                            return false;
                                        }
                                    });
                                    // close its tab
                                    w2ui['tabs_file_editor'].animateClose(tabId);
                                    break;
                                }
                            }
                            // hide tabs container if no tab opened
                            if ($('ul#fe-main-list-tab-content li').length === 0) {
                                $('#tabs_file_editor').hide();
                            }
                            // refresh tabs
                            w2ui['tabs_file_editor'].refresh();
                            break;
                        }
                        case "close_others":
                        {
                            console.log("close all tabs except tab: " + tabId);
                            // remove content from content list
                            listItems.each(function (idx, li) {
                                if ($(li).attr("data-node-id") != tabId) {
                                    $(li).remove();
                                }
                            });

                            for (var k = 0; k < w2ui['tabs_file_editor'].tabs.length; k++) {
                                var tabK_id = w2ui['tabs_file_editor'].tabs[k].id;
                                if (tabK_id != tabId) {
                                    // close its tab
                                    w2ui['tabs_file_editor'].animateClose(tabK_id);
                                }
                            }

                            w2ui['tabs_file_editor'].click(tabId);

                            // refresh tabs
                            w2ui['tabs_file_editor'].refresh();
                            break;
                        }
                        case "close_all":
                        {
                            console.log("close all tabs");
                            // remove content from content list
                            listItems.each(function (idx, li) {
                                $(li).remove();
                            });

                            for (var l = 0; l < w2ui['tabs_file_editor'].tabs.length; l++) {
                                var tabL_id = w2ui['tabs_file_editor'].tabs[l].id;
                                w2ui['tabs_file_editor'].animateClose(tabL_id);
                            }


                            // hide tabs container if no tab opened
                            if ($('ul#fe-main-list-tab-content li').length === 0) {
                                $('#tabs_file_editor').hide();
                            }

                            // refresh tabs
                            w2ui['tabs_file_editor'].refresh();
                            break;
                        }
                        default :
                        {
                            break;
                        }
                    }
                }
            },
            fileWrapper: {
                requests: {},// a dict that contains running ajax request per file for (synchro)
                sync_workers: {},// a dict that contains running worker for a certain file (synchro)
                syncFile: function (nodeId, projectUuid, content, is_compressed, is_chunked) {
                    var $editor_notif = $('#editor-notif-msg');
                    if (is_chunked === true) {
                        $editor_notif.html('saving changes...');
                        $editor_notif.show();
                        // set data-is-sync to false
                        $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").attr('data-is-sync', false);

                        var counter_success = 1;
                        var aborted = false;
                        var send_chunk = function (chunk) {
                            return $.ajax({
                                url: "/api/protected/updateFileContent",
                                data: {nodeId: nodeId, projectUuid: projectUuid, content: JSON.stringify(chunk), is_compressed: is_compressed, is_chunked: is_chunked},
                                method: 'POST',
                                timeout: 30000,
                                cache: false,
                                success: function (data, textStatus, xhr) {
                                    //console.log("success for chunk...." + xhr.status);
                                    switch (xhr.status) {
                                        case 204:
                                            var percent = Math.ceil((counter_success * 100) / chunk.total);
                                            $editor_notif.html('saving changes...' + percent + '%');
                                            counter_success += 1;
                                            break;
                                        case 200:
                                            // set data-is-sync to true
                                            $editor_notif.html('saving changes...100%');
                                            $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").attr('data-is-sync', true);
                                            $editor_notif.html('');
                                            $editor_notif.hide();
                                            delete projectEditor.fileWrapper.requests[projectUuid + "-" + nodeId];
                                            break;
                                    }
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    $editor_notif.html('');
                                    $editor_notif.hide();
                                    aborted = true;
                                    if (xhr.statusText === "abort") {
                                        console.log("request aborted : " + counter_success);
                                        return;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                abort: function () {
                                    aborted = true;
                                }
                            });
                        };

                        var requests = [];
                        for (var k = 0; k < content.length; k++) {
                            if (!aborted) {
                                requests.push(send_chunk(content[k]));
                                //setTimeout(send_chunk(content[k])
                                //    , k * 100);
                            }
                        }
                        content = null;
                        return requests;
                    } else {
                        return [$.ajax({
                            url: "/api/protected/updateFileContent",
                            data: {nodeId: nodeId, projectUuid: projectUuid, content: content, is_compressed: is_compressed, is_chunked: is_chunked},
                            method: 'POST',
                            timeout: 25000,
                            cache: false,
                            beforeSend: function () {
                                // show message => saving changes
                                $editor_notif.html('saving changes...');
                                $editor_notif.show();
                                // set data-is-sync to false
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").attr('data-is-sync', false);
                            },
                            success: function (data_return) {
                                // set data-is-sync to true
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").attr('data-is-sync', true);
                            },
                            error: function (xhr) {
                                var msg;
                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                    msg = xhr.statusText;
                                } else {
                                    msg = xhr.statusText + ": " + xhr.responseText;
                                }
                                notify(msg, "error", 7000);
                                // TODO maybe try again one more time ??
                            },
                            complete: function () {
                                $editor_notif.html('');
                                $editor_notif.hide();
                            }
                        })];
                    }
                },
                openNewFileInEditorTab: function (node, data, callbackOnTabOpened) {
                    if (!data) {
                        notify("Can't open file because, data received is empty!", "error", 5000);
                        return;
                    }
                    console.log("opening new tab  => : id:" + node.id);
                    // create tab tooltip
                    var nodePath = $('#jstree-tree-fileeditor').jstree().get_path(node).join("/").replace(/\s\(([\d]+)\)/g, "");
                    var nodePathSplit = nodePath.split("/");
                    nodePathSplit.shift();
                    // add tab
                    w2ui['tabs_file_editor'].add({ id: node.id, text: node.text, closable: true, tooltip: nodePathSplit.join("/")});
                    // scroll to right to make it visible if tabs overflow area
                    w2ui['tabs_file_editor'].scroll('right');
                    // set it as active
                    w2ui['tabs_file_editor'].select(node.id);

                    var mainTabListContent = $("ul#fe-main-list-tab-content");
                    var template, dataTmpl, html;

                    // populate template depending on file type
                    switch (node.type) {
                        case "edit_txt":
                        case "edit_smali":
                        case "edit_xml":
                        case "edit_js":
                        case "edit_json":
                        case "edit_html":
                        case "edit_css":
                        case "download":
                            template = $('#apkreverse-file-editable-template').html();
                            dataTmpl = {
                                node_id: node.id
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            mainTabListContent.append(html);
                            // instantiate codemirror code editor
                            var txtArea = document.getElementById('editor-' + node.id);
                            txtArea.value = data.fileContent;
                            var editor = CodeMirror.fromTextArea(txtArea, utils.get_editor_config(node.type, data.fileContent));
                            editor.on('change',
                                // when editor contents changed, delay server synchronization for 1000 milliseconds to prevent firing
                                // sync whenever a single character changed
                                debounce(function (cMirror) {
                                    var key = data.projectUuid + "-" + node.id;

                                    // aborting all ajax requests for the same file
                                    if (key in projectEditor.fileWrapper.requests
                                        && projectEditor.fileWrapper.requests[key]) {
                                        for (var j = 0; j < projectEditor.fileWrapper.requests[key].length; j++) {
                                            if (projectEditor.fileWrapper.requests[key][j]) {
                                                projectEditor.fileWrapper.requests[key][j].abort();
                                            }
                                        }
                                        projectEditor.fileWrapper.requests[key] = undefined;
                                        delete projectEditor.fileWrapper.requests[key];
                                        console.log('ABORTED ALL AJAX CALLS == > ' + node.text);
                                    }

                                    if (typeof(Worker) !== "undefined") {
                                        // Yes! Web worker support!

                                        // terminate existing worker for the same file
                                        if (key in projectEditor.fileWrapper.sync_workers
                                            && projectEditor.fileWrapper.sync_workers[key] !== undefined) {
                                            projectEditor.fileWrapper.sync_workers[key].terminate();
                                            projectEditor.fileWrapper.sync_workers[key] = undefined;
                                            delete projectEditor.fileWrapper.sync_workers[key];
                                            console.log('WORKER DESTROYED == > new call for ==> ' + node.text);
                                        }

                                        var worker_synchro = new Worker('/static/protected/js/apk_reverse_worker_editor.js');
                                        projectEditor.fileWrapper.sync_workers[key] = worker_synchro;
                                        worker_synchro.addEventListener('message', function (e) {
                                            projectEditor.fileWrapper.requests[key] = projectEditor.fileWrapper.syncFile(node.id,
                                                data.projectUuid, e.data.content, e.data.is_compressed, e.data.is_chunked);
                                            projectEditor.fileWrapper.sync_workers[key].terminate();
                                            projectEditor.fileWrapper.sync_workers[key] = undefined;
                                            delete projectEditor.fileWrapper.sync_workers[key];
                                            console.log('WORKER ENDED ==> ' + node.text);
                                        }, false);
                                        worker_synchro.postMessage({'cmd': 'sync_file', 'content': cMirror.getValue()});
                                    } else {
                                        // Sorry! No Web Worker support..
                                        var content = btoa(pako.gzip(cMirror.getValue().split("\n").join("%%br%%"), { to: 'string' }));
                                        var orig_length = cMirror.getValue().length;
                                        var compressed_length = content.length;
                                        console.log("original[" + orig_length + "], compressed [" + compressed_length + "} ==> compression rate = " + Math.ceil((compressed_length * 100) / orig_length) + "%");

                                        var is_compressed = true;
                                        var is_chunked = false;
                                        var chunk_size = 170000;
                                        // chunk the file if its too big
                                        if (content.length > chunk_size) {
                                            var chunks = [];
                                            var nbChunks = Math.ceil(content.length / chunk_size);
                                            var uuid_ = generateUUID();
                                            for (var k = 0; k < nbChunks; k++) {
                                                var chunk = {};
                                                chunk['data'] = content.substr(k * chunk_size, chunk_size);
                                                chunk['uuid'] = uuid_; // unique chunk identifier
                                                chunk['pos'] = k; // position
                                                chunk['total'] = nbChunks; // total chunks fr this file
                                                chunks.push(chunk);
                                            }
                                            is_chunked = true;
                                            content = chunks;
                                        }
                                        projectEditor.fileWrapper.requests[key] = projectEditor.fileWrapper.syncFile(node.id, data.projectUuid, content, is_compressed, is_chunked);
                                    }
                                }, 1000)
                            );
                            editor.on('cursorActivity', function (cMirror) {
                                // clear all markers
                                if (projectEditor.cmMarkers.length !== 0) {
                                    for (var l = 0; l < projectEditor.cmMarkers.length; l++) {
                                        projectEditor.cmMarkers[l].clear();
                                    }
                                    projectEditor.cmMarkers.length = 0;
                                }
                            });
                            if (projectEditor.codeEditors.hasOwnProperty(node.id)) {
                                delete projectEditor.codeEditors[node.id];
                            }
                            projectEditor.codeEditors[node.id] = editor;
                            break;
                        case "load_img":
                            template = $('#apkreverse-file-loadable-img-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: data.imageWidth + "x" + data.imageHeight + " - " + formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            mainTabListContent.append(html);
                            $("ul#fe-main-list-tab-content li[data-node-id='" + node.id + "']").find('img.pannable-image').ImageViewer();
                            break;
                        case "load_audio":
                            template = $('#apkreverse-file-loadable-audio-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                mimeType: node.data.mimeType,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            mainTabListContent.append(html);
                            break;
                        case "load_video":
                            template = $('#apkreverse-file-loadable-video-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                mimeType: node.data.mimeType,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            mainTabListContent.append(html);
                            break;
                        case "load_pdf":
                            template = $('#apkreverse-file-pdf-template').html();
                            dataTmpl = {
                                node_id: data.nodeId,
                                base64PDF: data.fileContent
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            mainTabListContent.append(html);
                            break;
                        default:
                            break;
                    }


                    var listItems = $("ul#fe-main-list-tab-content li");
                    listItems.each(function (idx, li) {
                        if ($(li).attr("data-node-id") === node.id) {
                            $(li).removeClass('hidden');
                        } else {
                            $(li).removeClass('hidden').addClass('hidden');
                        }
                    });

                    if ($('ul#fe-main-list-tab-content li').length > 0) {
                        $('#tabs_file_editor').show();
                    }
                    if (callbackOnTabOpened) {
                        callbackOnTabOpened.call();
                    }
                },
                updateTabContent: function (data) { // update an opened tab content with new content
                    if (!data) {
                        notify("Can't open file because, data received is empty!", "error", 5000);
                        return;
                    }
                    var tree = $('#jstree-tree-fileeditor');
                    var nodeId = data.nodeId;
                    var node = tree.jstree().get_node(nodeId);

                    console.log("updating tab  => : id:" + node.id);

                    //var mainTabListContent = $("ul#fe-main-list-tab-content");
                    var template, dataTmpl, html;
                    var $li_to_replace = $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']");
                    var has_hidden_class = $li_to_replace.hasClass('hidden');


                    // populate template depending on file type
                    switch (node.type) {
                        case "edit_txt":
                        case "edit_smali":
                        case "edit_xml":
                        case "edit_js":
                        case "edit_json":
                        case "edit_html":
                        case "edit_css":
                        case "download":
                        {
                            template = $('#apkreverse-file-editable-template').html();
                            dataTmpl = {
                                node_id: node.id
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            $li_to_replace.replaceWith(html);
                            // instantiate codemirror code editor
                            var txtArea = document.getElementById('editor-' + node.id);
                            txtArea.value = data.fileContent;
                            var editor = CodeMirror.fromTextArea(txtArea, utils.get_editor_config(node.type, data.fileContent));
                            editor.on('change',
                                // when editor contents changed, delay server synchronization for 1000 milliseconds to prevent firing
                                // sync whenever a single character changed
                                debounce(function (cMirror) {
                                    var key = data.projectUuid + "-" + node.id;

                                    // aborting all ajax requests for the same file
                                    if (key in projectEditor.fileWrapper.requests
                                        && projectEditor.fileWrapper.requests[key]) {
                                        for (var j = 0; j < projectEditor.fileWrapper.requests[key].length; j++) {
                                            if (projectEditor.fileWrapper.requests[key][j]) {
                                                projectEditor.fileWrapper.requests[key][j].abort();
                                            }
                                        }
                                        projectEditor.fileWrapper.requests[key] = undefined;
                                        delete projectEditor.fileWrapper.requests[key];
                                        console.log('ABORTED ALL AJAX CALLS == > ' + node.text);
                                    }

                                    if (typeof(Worker) !== "undefined") {
                                        // Yes! Web worker support!

                                        // terminate existing worker for the same file
                                        if (key in projectEditor.fileWrapper.sync_workers
                                            && projectEditor.fileWrapper.sync_workers[key] !== undefined) {
                                            projectEditor.fileWrapper.sync_workers[key].terminate();
                                            projectEditor.fileWrapper.sync_workers[key] = undefined;
                                            delete projectEditor.fileWrapper.sync_workers[key];
                                            console.log('WORKER DESTROYED == > new call for ==> ' + node.text);
                                        }

                                        var worker_synchro = new Worker('/static/protected/js/apk_reverse_worker_editor.js');
                                        projectEditor.fileWrapper.sync_workers[key] = worker_synchro;
                                        worker_synchro.addEventListener('message', function (e) {
                                            projectEditor.fileWrapper.requests[key] = projectEditor.fileWrapper.syncFile(node.id,
                                                data.projectUuid, e.data.content, e.data.is_compressed, e.data.is_chunked);
                                            projectEditor.fileWrapper.sync_workers[key].terminate();
                                            projectEditor.fileWrapper.sync_workers[key] = undefined;
                                            delete projectEditor.fileWrapper.sync_workers[key];
                                            console.log('WORKER ENDED ==> ' + node.text);
                                        }, false);
                                        worker_synchro.postMessage({'cmd': 'sync_file', 'content': cMirror.getValue()});
                                    } else {
                                        // Sorry! No Web Worker support..
                                        var content = btoa(pako.gzip(cMirror.getValue().split("\n").join("%%br%%"), { to: 'string' }));
                                        var orig_length = cMirror.getValue().length;
                                        var compressed_length = content.length;
                                        console.log("original[" + orig_length + "], compressed [" + compressed_length + "} ==> compression rate = " + Math.ceil((compressed_length * 100) / orig_length) + "%");

                                        var is_compressed = true;
                                        var is_chunked = false;
                                        var chunk_size = 170000;
                                        // chunk the file if its too big
                                        if (content.length > chunk_size) {
                                            var chunks = [];
                                            var nbChunks = Math.ceil(content.length / chunk_size);
                                            var uuid_ = generateUUID();
                                            for (var k = 0; k < nbChunks; k++) {
                                                var chunk = {};
                                                chunk['data'] = content.substr(k * chunk_size, chunk_size);
                                                chunk['uuid'] = uuid_; // unique chunk identifier
                                                chunk['pos'] = k; // position
                                                chunk['total'] = nbChunks; // total chunks fr this file
                                                chunks.push(chunk);
                                            }
                                            is_chunked = true;
                                            content = chunks;
                                        }
                                        projectEditor.fileWrapper.requests[key] = projectEditor.fileWrapper.syncFile(node.id, data.projectUuid, content, is_compressed, is_chunked);
                                    }
                                }, 1000)
                            );
                            editor.on('cursorActivity', function (cMirror) {
                                // clear all markers
                                if (projectEditor.cmMarkers.length !== 0) {
                                    for (var l = 0; l < projectEditor.cmMarkers.length; l++) {
                                        projectEditor.cmMarkers[l].clear();
                                    }
                                    projectEditor.cmMarkers.length = 0;
                                }
                            });

                            if (projectEditor.codeEditors.hasOwnProperty(node.id)) {
                                delete projectEditor.codeEditors[node.id];
                            }

                            projectEditor.codeEditors[node.id] = editor;

                            if (has_hidden_class) {
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").addClass('hidden');
                            }
                            break;
                        }
                        case "load_img":
                        {
                            template = $('#apkreverse-file-loadable-img-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: data.imageWidth + "x" + data.imageHeight + " - " + formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            $li_to_replace.replaceWith(html);
                            if (has_hidden_class) {
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").addClass('hidden');
                            }
                            $li_to_replace.find('img.pannable-image').ImageViewer();
                            break;
                        }
                        case "load_audio":
                        {
                            template = $('#apkreverse-file-loadable-audio-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                mimeType: node.data.mimeType,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            $li_to_replace.replaceWith(html);
                            if (has_hidden_class) {
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").addClass('hidden');
                            }
                            break;
                        }
                        case "load_video":
                        {
                            template = $('#apkreverse-file-loadable-video-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                mimeType: node.data.mimeType,
                                url_src: "/api/protected/mediaStreamer?nodeId=" + node.id + "&projectUuid=" + data.projectUuid + "&dummy=" + generateUUID(),
                                file_info: formatBytes(data.fileSize)
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            $li_to_replace.replaceWith(html);
                            if (has_hidden_class) {
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").addClass('hidden');
                            }
                            break;
                        }
                        case "load_pdf":
                        {
                            template = $('#apkreverse-file-pdf-template').html();
                            dataTmpl = {
                                node_id: node.id,
                                base64PDF: data.fileContent
                            };
                            html = Mustache.to_html(template, dataTmpl);
                            $li_to_replace.replaceWith(html);
                            if (has_hidden_class) {
                                $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']").addClass('hidden');
                            }
                            break;
                        }
                        default:
                        {
                            break;
                        }
                    }

                    /*
                     var listItems = $("ul#fe-main-list-tab-content li");
                     listItems.each(function (idx, li) {
                     if ($(li).attr("data-node-id") === node.id) {
                     $(li).removeClass('hidden');
                     } else {
                     $(li).removeClass('hidden').addClass('hidden');
                     }
                     });
                     */

                    if ($('ul#fe-main-list-tab-content li').length > 0) {
                        $('#tabs_file_editor').show();
                    }
                }
            },
            contextMenuWrapper: {
                createFolderItem: function (node) {
                    console.log("create new folder on node : " + node.text);
                    // get node path
                    var tree = $('#jstree-tree-fileeditor');
                    var nodePath = tree.jstree().get_path(node).join("/").replace(/\s\(([\d]+)\)/g, "");
                    var nodePathSplit = nodePath.split("/");
                    nodePathSplit.shift();
                    var path_ = nodePathSplit.join("/");
                    var pathParent = path_ === "" ? "/project/" : path_ + "/";

                    // build the popup window
                    var title;
                    if (node.type === 'folder') {
                        //filename = node.text.replace(/\s\(([\d]+)\)/g, "");
                        //title = 'Create new folder inside: ' + filename;
                        title = 'Create new folder inside: ';
                    } else {
                        notify("Please select a folder!");
                        return;
                    }
                    w2popup.open({
                        title: title,
                        body: '<div class="w2ui-centered form-group">' +
                            '<span>' + pathParent + '</span><br><br>' +
                            '<input type="text" class="form-control" id="input-node-create-new-folder" style="background: #d1d7e6;" value="" placeholder="Enter folder name here..."/>' +
                            '</div>',

                        buttons: '<button class="w2ui-btn" onclick="ApkToolModule.createNewFolder(' + node.id + ');">Create</button>' +
                            '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                        width: 500,
                        height: 300,
                        overflow: 'hidden',
                        color: '#000',
                        speed: '0.3',
                        opacity: '0.4',
                        modal: true,
                        showClose: true,
                        //showMax: true,
                        onOpen: function (event) {
                            //console.log('open');
                        },
                        onClose: function (event) {
                            //console.log('close');
                        },
                        onKeydown: function (event) {
                        }
                    });

                    var $input_node_create_new_folder = $("#input-node-create-new-folder");
                    $input_node_create_new_folder.keyup(function (event) {
                        var code = event.keyCode || event.which;
                        if (code == '13') { // enter button pressed
                            ApkToolModule.createNewFolder(node.id);
                        }
                    });
                    setTimeout(function () {
                        $input_node_create_new_folder.focus();
                    }, 300)
                },
                createFolderItem_execute_create: function (nodeId) {
                    // nodeId is the id of the parent that will contain the new folder
                    console.log('creating new folder inside ' + nodeId);
                    var new_folder_name = $("#input-node-create-new-folder").val().trim();
                    var tree = $('#jstree-tree-fileeditor');
                    var parentNode = tree.jstree().get_node(nodeId);

                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        data: {action: 'CREATE_NEW_FOLDER', projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'), nodeId: nodeId, new_folder_name: new_folder_name},
                        type: 'POST',
                        timeout: 20000,
                        cache: false,
                        beforeSend: function () {
                            w2popup.lock('Creating new folder...', true);
                        },
                        success: function (data) {
                            // create the new node inside our jsTree
                            $("#jstree-tree-fileeditor").jstree(true).create_node(nodeId, data, "first", function () {
                                // if parent is loaded and opened => deselect all + select new folder + focus the new created folder
                                if (parentNode.state.loaded === true && parentNode.state.opened === true) {
                                    tree.jstree("deselect_all");
                                    tree.jstree('select_node', data.id);
                                    // bring focus to corresponding jstree node with a scroll animation
                                    var nodeLine = tree.jstree(true).get_node(data.id, true).children('.jstree-anchor');
                                    var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                                    if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport => no scroll
                                        nodeLine.focus();
                                    } else { // if not not visible inside viewport => scroll with animation
                                        container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                            nodeLine.focus();
                                        });
                                    }
                                }
                            }, true);
                            // update its parent size if parent is not the root folder (increment it by one)
                            if (nodeId != '#' && nodeId != '0') {
                                var sizeArray = parentNode.text.match(/\([\d]+\)/g).map(function (val) {
                                    return val.replace(/\(|\)/g, '');
                                });

                                if (sizeArray.length == 1) {
                                    try {
                                        var newSize = parseInt(sizeArray[0]) + 1;
                                        var parentNewName = parentNode.text.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize + ')';
                                        tree.jstree(true).rename_node(parentNode, parentNewName);
                                    } catch (err) {
                                        // do nothing
                                        console.log(err.toString());
                                    }
                                }
                            }

                            w2popup.close();
                            notify("Folder created with success", "success", 5000);
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        },
                        complete: function () {
                            w2popup.unlock();
                        }
                    });
                },
                openItem: function (node) {
                    console.log("opening node : " + node.text);
                    projectEditor.tabsManager.openTab(node, $('#project-editor-devenvironment-data').attr('data-project-uuid'));
                },
                renameItem: function (node) {
                    console.log("reaming node : " + node.text);
                    var title, filename;
                    if (node.type === 'folder') {
                        filename = node.text.replace(/\s\(([\d]+)\)/g, "");
                        title = 'Rename folder: ' + filename;
                    } else {
                        filename = node.text;
                        title = 'Rename file: ' + filename;
                    }
                    w2popup.open({
                        title: title,
                        body: '<div class="w2ui-centered form-group">' +
                            '<label for="pwd" style="margin-bottom: 10px;width: 100%;text-align: left;">Enter new name :</label>' +
                            '<input type="text" class="form-control" id="input-node-rename" style="background: #d1d7e6;" value="' + filename.trim() + '"/>' +
                            '</div>',

                        buttons: '<button class="w2ui-btn" onclick="ApkToolModule.renameFileOrFolder(' + node.id + ');">Rename</button>' +
                            '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                        width: 500,
                        height: 300,
                        overflow: 'hidden',
                        color: '#000',
                        speed: '0.3',
                        opacity: '0.4',
                        modal: true,
                        showClose: true,
                        //showMax: true,
                        onOpen: function (event) {
                            //console.log('open');
                        },
                        onClose: function (event) {
                            //console.log('close');
                        },
                        onKeydown: function (event) {
                        }
                    });
                    var $input_node_rename = $("#input-node-rename");
                    $input_node_rename.keyup(function (event) {
                        var code = event.keyCode || event.which;
                        if (code == '13') { // enter button pressed
                            ApkToolModule.renameFileOrFolder(node.id);
                        }
                    });
                    setTimeout(function () {
                        $input_node_rename.focus();
                    }, 300);

                },
                renameItem_execute_rename: function (nodeId) {
                    var new_name = $("#input-node-rename").val().trim();
                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        data: {action: 'RENAME_NODE', projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'), nodeId: nodeId, new_name: new_name},
                        type: 'POST',
                        timeout: 120000,
                        cache: false,
                        beforeSend: function () {
                            w2popup.lock('Renaming element...', true);
                        },
                        success: function (data) {
                            var tree = $('#jstree-tree-fileeditor');
                            var current_node = tree.jstree().get_node(nodeId);
                            var old_name = current_node.text;

                            if (current_node.type === 'folder') {
                                // folder rename => its new name must contain its size between parenthesis
                                var old_folder_size = old_name.match(/\s\(([\d]+)\)/g)[0];
                                var new_folder_name = new_name + ' ' + old_folder_size;
                                tree.jstree(true).rename_node(nodeId, new_folder_name);

                                // check if any of opened files is a child of this folder => change its tooltip
                                if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                    for (var k = 0; k < w2ui['tabs_file_editor'].tabs.length; k++) {
                                        var tmpNodeId = w2ui['tabs_file_editor'].tabs[k].id;
                                        var tmpNode = tree.jstree().get_node(tmpNodeId);
                                        if (tmpNode.parents.indexOf(String(nodeId)) > -1) {
                                            // update tab tooltip
                                            var nodePath1 = tree.jstree().get_path(tmpNode).join("/").replace(/\s\(([\d]+)\)/g, "");
                                            var nodePathSplit1 = nodePath1.split("/");
                                            nodePathSplit1.shift();
                                            w2ui['tabs_file_editor'].tabs[k].tooltip = nodePathSplit1.join("/");
                                        }
                                    }
                                    // refresh tabs
                                    w2ui['tabs_file_editor'].refresh();
                                }
                            } else {
                                // if it is a file => rename it
                                tree.jstree(true).rename_node(nodeId, new_name);
                                // if its is opened => change its title and tooltip
                                if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                    var tabContent = $("ul#fe-main-list-tab-content li[data-node-id='" + nodeId + "']");
                                    if (tabContent.length > 0) {
                                        console.log("tab already opened");
                                        for (var l = 0; l < w2ui['tabs_file_editor'].tabs.length; l++) {
                                            console.log("compare tab[" + l + "].id=[" + w2ui['tabs_file_editor'].tabs[l].id + "] to nodeId=[" + nodeId + "]");
                                            if (w2ui['tabs_file_editor'].tabs[l].id == nodeId) {
                                                console.log("found it");
                                                // update tab text
                                                w2ui['tabs_file_editor'].tabs[l].text = new_name;
                                                // update tab tooltip
                                                var nodePath = tree.jstree().get_path(current_node).join("/").replace(/\s\(([\d]+)\)/g, "");
                                                var nodePathSplit = nodePath.split("/");
                                                nodePathSplit.shift();
                                                w2ui['tabs_file_editor'].tabs[l].tooltip = nodePathSplit.join("/");
                                                // select tab and refresh it
                                                w2ui['tabs_file_editor'].refresh();
                                                w2ui['tabs_file_editor'].click(nodeId);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            w2popup.close();
                            // bring focus to corresponding jstree node with a scroll animation
                            tree.jstree("deselect_all");
                            tree.jstree('select_node', nodeId);
                            // bring focus to corresponding jstree node with a scroll animation
                            var nodeLine = tree.jstree(true).get_node(nodeId, true).children('.jstree-anchor');
                            var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                            if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport => no scroll
                                nodeLine.focus();
                            } else { // if not not visible inside viewport => scroll with animation
                                container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                    nodeLine.focus();
                                });
                            }
                            notify("Element renamed with success", "success", 5000);
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        },
                        complete: function () {
                            w2popup.unlock();
                        }
                    });
                },
                replaceItem: function (node) {
                    console.log("replacing node : " + node.text);
                    // open "dropzone replace by", set its accepted files extension,
                    // folder path
                    var tree = $('#jstree-tree-fileeditor');
                    var nodePath = tree.jstree().get_path(node).join("/").replace(/\s\(([\d]+)\)/g, "");
                    var nodePathSplit = nodePath.split("/");
                    nodePathSplit.shift();
                    var path_ = nodePathSplit.join("/");
                    var path = path_ === "" ? "project" : path_;

                    // temp folder name
                    var tempFolderName = generateUUID();
                    if (node.type !== 'folder') {
                        // clean the dropzone => remove all files
                        Dropzone.options.replaceFileDropzone.clean();
                        $('#modalReplaceFile-temp-folder-name').val(tempFolderName);
                        $('#modalReplaceFile-target-nodeId').val(node.id);
                        $('#modalReplaceFile-file-path').text(path);
                        $('#modalReplaceFile').modal('show');
                    } else {
                        notify("Please select a file!", "error", 5000);
                    }
                },
                deleteItem: function (node) {
                    console.log("deleting node : " + node.text);
                    // get node path
                    var tree = $('#jstree-tree-fileeditor');
                    var nodePath = tree.jstree().get_path(node).join("/").replace(/\s\(([\d]+)\)/g, "");
                    var nodePathSplit = nodePath.split("/");
                    nodePathSplit.shift();
                    var path = nodePathSplit.join("/");

                    // build the popup window
                    var title, filename, type;
                    if (node.type === 'folder') {
                        type = 'folder';
                        filename = node.text.replace(/\s\(([\d]+)\)/g, "");
                        title = 'Delete folder: ' + filename;
                    } else {
                        type = 'file';
                        filename = node.text;
                        title = 'Delete file: ' + filename;
                    }
                    w2popup.open({
                        title: title,
                        body: '<div class="w2ui-centered form-group">' +
                            '<span>Are you sure you want to delete this ' + type + ' ?</span>' +
                            '<br><br><span>' + path + '</span>' +
                            '</div>',

                        buttons: '<button class="w2ui-btn" onclick="ApkToolModule.deleteFileOrFolder(' + node.id + ');">Delete</button>' +
                            '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                        width: 500,
                        height: 300,
                        overflow: 'hidden',
                        color: '#000',
                        speed: '0.3',
                        opacity: '0.4',
                        modal: true,
                        showClose: true,
                        //showMax: true,
                        onOpen: function (event) {
                            //console.log('open');
                        },
                        onClose: function (event) {
                            //console.log('close');
                        },
                        onKeydown: function (event) {
                        }
                    });
                },
                deleteItem_execute_delete: function (nodeToRemoveId) {
                    console.log("start deleting node : " + nodeToRemoveId);
                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        data: {action: 'DELETE_NODE', projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'), nodeId: nodeToRemoveId},
                        type: 'POST',
                        timeout: 120000,
                        cache: false,
                        beforeSend: function () {
                            w2popup.lock('Deleting element...', true);
                        },
                        success: function (data) {
                            var tree = $('#jstree-tree-fileeditor');
                            // get node from nodeId
                            var node_to_remove = tree.jstree().get_node(nodeToRemoveId);
                            var parent_of_node_to_remove = tree.jstree().get_node(node_to_remove.parents[0]);
                            var parentName = parent_of_node_to_remove.text;
                            var listItems = $("ul#fe-main-list-tab-content li");

                            if (node_to_remove.type === 'folder') {
                                // check if any of opened files in the editor is a descendant of the removed folder
                                // if so => close it
                                if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                    for (var k = 0; k < w2ui['tabs_file_editor'].tabs.length; k++) {
                                        var tmpNodeId = w2ui['tabs_file_editor'].tabs[k].id;
                                        var tmpNode = tree.jstree().get_node(tmpNodeId);
                                        console.log('tab: ' + k + ', parent of ' + tmpNodeId + ', are: ' + tmpNode.parents + ', and target is: ' + nodeToRemoveId);
                                        if (tmpNode.parents.indexOf(String(nodeToRemoveId)) > -1) {
                                            console.log('found match => removing tab: ' + k + ' for node: ' + tmpNodeId);
                                            // remove content from content list
                                            listItems.each(function (idx, li) {
                                                if ($(li).attr("data-node-id") == tmpNodeId) {
                                                    if (!$(li).hasClass('hidden')) {
                                                        var next_id = $(li).next('li').attr("data-node-id");
                                                        var prev_id = $(li).prev('li').attr("data-node-id");
                                                        if (next_id) {
                                                            w2ui['tabs_file_editor'].click(next_id)
                                                        } else if (prev_id) {
                                                            w2ui['tabs_file_editor'].click(prev_id)
                                                        }
                                                    }
                                                    $(li).remove();
                                                    return false;
                                                }
                                            });
                                            // close it
                                            w2ui['tabs_file_editor'].animateClose(tmpNodeId);
                                        }
                                    }
                                    if ($('ul#fe-main-list-tab-content li').length === 0) {
                                        $('#tabs_file_editor').hide();
                                    }
                                    // refresh tabs
                                    w2ui['tabs_file_editor'].refresh();
                                }
                                // update its parent size if parent is not the root folder
                                if (parent_of_node_to_remove.id != '#' && parent_of_node_to_remove.id != '0') {
                                    var sizeArray = parentName.match(/\([\d]+\)/g).map(function (val) {
                                        return val.replace(/\(|\)/g, '');
                                    });

                                    if (sizeArray.length == 1) {
                                        try {
                                            var newSize = parseInt(sizeArray[0]) - 1;
                                            var parentNewName = parentName.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize + ')';
                                            tree.jstree(true).rename_node(parent_of_node_to_remove.id, parentNewName);
                                        } catch (err) {
                                            // do nothing
                                            console.log(err.toString());
                                        }
                                    }
                                }
                            } else { // file was remove
                                // check if file is opened in the editor
                                // if so => close it
                                if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                    for (var j = 0; j < w2ui['tabs_file_editor'].tabs.length; j++) {
                                        if (w2ui['tabs_file_editor'].tabs[j].id == nodeToRemoveId) {
                                            // remove content from content list
                                            listItems.each(function (idx, li) {
                                                if ($(li).attr("data-node-id") == nodeToRemoveId) {
                                                    if (!$(li).hasClass('hidden')) {
                                                        var next_id = $(li).next('li').attr("data-node-id");
                                                        var prev_id = $(li).prev('li').attr("data-node-id");
                                                        if (next_id) {
                                                            w2ui['tabs_file_editor'].click(next_id)
                                                        } else if (prev_id) {
                                                            w2ui['tabs_file_editor'].click(prev_id)
                                                        }
                                                    }
                                                    $(li).remove();
                                                    return false;
                                                }
                                            });
                                            // close its tab
                                            w2ui['tabs_file_editor'].animateClose(nodeToRemoveId);
                                            break;
                                        }
                                    }
                                    if ($('ul#fe-main-list-tab-content li').length === 0) {
                                        $('#tabs_file_editor').hide();
                                    }
                                    // refresh tabs
                                    w2ui['tabs_file_editor'].refresh();
                                }

                                // update its parent size if parent is not the root folder
                                if (parent_of_node_to_remove.id != '#' && parent_of_node_to_remove.id != '0') {
                                    var sizeArray_ = parentName.match(/\([\d]+\)/g).map(function (val) {
                                        return val.replace(/\(|\)/g, '');
                                    });

                                    if (sizeArray_.length == 1) {
                                        try {
                                            var newSize_ = parseInt(sizeArray_[0]) - 1;
                                            var parentNewName_ = parentName.replace(/\s\(([\d]+)\)/g, "") + ' (' + newSize_ + ')';
                                            tree.jstree(true).rename_node(parent_of_node_to_remove.id, parentNewName_);
                                        } catch (err) {
                                            // do nothing
                                            console.log(err.toString());
                                        }
                                    }
                                }
                            }
                            // remove node
                            tree.jstree(true).delete_node(nodeToRemoveId);
                            w2popup.close();
                            notify("Element removed with success", "success", 5000);
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        },
                        complete: function () {
                            w2popup.unlock();
                        }
                    });
                },
                addFilesItem: function (node) {
                    console.log("adding files to node: " + node.text);
                    // folder path
                    var tree = $('#jstree-tree-fileeditor');
                    var nodePath = tree.jstree().get_path(node).join("/").replace(/\s\(([\d]+)\)/g, "");
                    var nodePathSplit = nodePath.split("/");
                    nodePathSplit.shift();
                    var path_ = nodePathSplit.join("/");
                    var path = path_ === "" ? "project" : path_;

                    // temp folder name
                    var tempFolderName = generateUUID();
                    if (node.type === 'folder') {
                        // clean the dropzone => remove all files
                        Dropzone.options.addFilesToProjectFolderDropzone.clean();
                        $('#modalAddFilesToProjectFolder-temp-folder-name').val(tempFolderName);
                        $('#modalAddFilesToProjectFolder-target-nodeId').val(node.id);
                        $('#modalAddFilesToProjectFolder-folder-path').text(path);
                        $('#modalAddFilesToProjectFolder').modal('show');
                    } else {
                        notify("Please select a folder!", "error", 5000);
                    }
                },
                downloadItem: function (node) {
                    try {
                        // remove previous iframe
                        var previousDownloadFrame = document.getElementById('iframe-fe-file-download');
                        if (previousDownloadFrame !== null) {
                            previousDownloadFrame.parentNode.removeChild(previousDownloadFrame);
                        }
                        // add new iframe
                        var downloadFrame = document.createElement("iframe");
                        downloadFrame.id = 'iframe-fe-file-download';
                        downloadFrame.setAttribute('class', "downloadFrameScreen");
                        var url = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port
                            + "/api/protected/contextMenuForEditor/downloadFile?projectUuid=" + $('#project-editor-devenvironment-data').attr('data-project-uuid') + "&nodeId=" + node.id;
                        downloadFrame.setAttribute('src', url);
                        document.body.appendChild(downloadFrame);
                    } catch (err) {
                        notify(err.toString(), "error", 7000);
                    }
                },
                showInExplorerItem: function (nodeId) {
                    console.log("show in explorer, nodeId: " + nodeId);
                    projectEditor.folderExplorer.showInExplorer(nodeId);
                }
            },
            cancelUserProcess: function (button) {
                RevEnge.minMCfg();

                var $button = $(button);
                var procId = $button.attr('data-process-id');
                var procType = $button.attr('data-process-type');

                w2confirm('Do want to cancel ' + procType + ' process?')
                    .yes(function () {
                        if (procId) {
                            // ajax cancel process from server
                            $.ajax({
                                url: "/api/protected/cancelAplToolProcessHandler",
                                data: {processId: procId},
                                type: 'POST',
                                timeout: 180000,
                                cache: false,
                                beforeSend: function () {
                                    showBusysign();
                                    $button.attr('disabled', true);
                                },
                                success: function (data) {
                                    var id = $button.attr('id');
                                    $button.attr('data-process-id', '');
                                    $button.attr('data-process-type', '');
                                    $button.hide();

                                    if (id === 'cancel-text-process') {
                                        var $div_search_cnt = $('#feBottomTabSearch-content');
                                        $div_search_cnt.append('<div> </div>');
                                        $div_search_cnt.append('<div>************************</div>');
                                        $div_search_cnt.append('<div>*** Process canceled ***</div>');
                                        $div_search_cnt.append('<div>************************</div>');
                                        $div_search_cnt.append('<div> </div>');
                                        $div_search_cnt.append('<div>' + procType + ' >>> ' + data.message + '</div>');

                                        var scrollPosition = $div_search_cnt.height();
                                        $('ul#fe-bottom-list-tab-content').scrollTop(scrollPosition);// scroll to bottom => new content always visible
                                        $div_search_cnt.attr('data-scroll', scrollPosition);
                                    } else if (id === 'cancel-log-process') {
                                        var $div_log_cnt = $('#feBottomTabLog-content');
                                        $div_log_cnt.append('<div> </div>');
                                        $div_log_cnt.append('<div>************************</div>');
                                        $div_log_cnt.append('<div>*** Process canceled ***</div>');
                                        $div_log_cnt.append('<div>************************</div>');
                                        $div_log_cnt.append('<div> </div>');
                                        $div_log_cnt.append('<div>' + procType + ' >>> ' + data.message + '</div>');

                                        var scrollPositionLog = $div_log_cnt.height();
                                        $('ul#fe-bottom-list-tab-content').scrollTop(scrollPositionLog);// scroll to bottom => new content always visible
                                        $div_log_cnt.attr('data-scroll', scrollPositionLog);
                                    }
                                },
                                error: function (xhr) {
                                    var msg;
                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                        msg = xhr.statusText;
                                    } else {
                                        msg = xhr.statusText + ": " + xhr.responseText;
                                    }
                                    notify(msg, "error", 7000);
                                },
                                complete: function () {
                                    $button.attr('disabled', false);
                                    hideBusysign();
                                }
                            });
                        } else {
                            $button.attr('data-process-id', '');
                            $button.attr('data-process-type', '');
                            $button.hide();
                        }
                    })
                    .no(function () {
                    });
            },
            getProjectInfo: function () {
                $.ajax({
                    url: "/api/protected/getProjectInfoForEditor",
                    data: {projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'GET',
                    dataType: "json",
                    timeout: 30000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var $infoContainer = $("div#project-info-container");
                        var $modal = $("#modalProjectInfo");
                        var template = $('#apkreverse-project-info-template').html();
                        var dataTemplate = {
                            response: data,
                            date_created_ago: jQuery.timeago(Date.parse(data.dateCreatedFormatted))
                        };
                        var html = Mustache.to_html(template, dataTemplate);
                        $infoContainer.empty();
                        $infoContainer.html(html);
                        $modal.removeData('bs.modal');
                        $modal.modal('show');
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            textSearchClicked: function () {// open search modal
                w2popup.open({
                    title: 'Text search',
                    body: '<div class="w2ui-centered form-group">' +
                        '<label for="input-fe-text-search" style="margin-bottom: 10px;width: 100%;text-align: left;">Enter text search:</label>' +
                        '<input type="text" class="form-control" maxlength="60" id="input-fe-text-search" style="background: #d1d7e6;" placeholder="text search here..." value=""/>' +
                        '<div style="text-align: left;margin-top: 10px;"><input type="checkbox" id="check-fe-text-search-sensitive" name="check_fe_text_search_sensitive">&nbsp;case sensitive</div>' +
                        '</div>',

                    buttons: '<button class="w2ui-btn" onclick="ApkToolModule.textSearchPerform();">Find</button>' +
                        '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                    width: 400,
                    height: 280,
                    overflow: 'hidden',
                    color: '#000',
                    speed: '0.3',
                    opacity: '0.4',
                    modal: true,
                    showClose: true,
                    //showMax: true,
                    onOpen: function (event) {
                        //console.log('open');
                    },
                    onClose: function (event) {
                        //console.log('close');
                    },
                    onKeydown: function (event) {
                    }
                });
                var $input_text_search = $("#input-fe-text-search");
                $input_text_search.keyup(function (event) {
                    var code = event.keyCode || event.which;
                    if (code == '13') { // enter button pressed
                        ApkToolModule.textSearchPerform();
                    }
                });
                setTimeout(function () {
                    $input_text_search.focus();
                }, 300);
            },
            textSearchPerform: function () {
                var searchQuery = $("#input-fe-text-search").val();
                console.log("searching for: " + searchQuery);

                $.ajax({
                    url: "/api/protected/textSearchForEditor",
                    data: {action: "SEARCH", searchQuery: searchQuery, caseSensitive: $('#check-fe-text-search-sensitive').is(":checked"), projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                            w2ui['layout_file_editor'].show('bottom', window.instant);
                        }
                        w2ui['bottom_file_editor'].click('feBottomTabSearch');
                    },
                    success: function (data) {
                        w2popup.close();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        //hideBusysign();
                    }
                });
            },
            textSearchAndReplaceClicked: function () {
                w2popup.open({
                    title: 'Text replace',
                    body: '<div class="w2ui-centered form-group">' +
                        '<label for="input-fe-text-search-n-replace-f" style="margin-bottom: 10px;width: 100%;text-align: left;">Enter text search:</label>' +
                        '<input type="text" class="form-control" maxlength="60" id="input-fe-text-search-n-replace-f" style="background: #d1d7e6;" placeholder="text search here..." value=""/>' +
                        '<br>' +
                        '<label for="input-fe-text-search-n-replace-r" style="margin-bottom: 10px;width: 100%;text-align: left;">Enter replacement text:</label>' +
                        '<input type="text" class="form-control" maxlength="60" id="input-fe-text-search-n-replace-r" style="background: #d1d7e6;" placeholder="text replacement here..." value=""/>' +
                        '<div style="text-align: left;margin-top: 10px;"><input type="checkbox" id="check-fe-text-replace-sensitive" name="check_fe_text_replace_sensitive" checked>&nbsp;case sensitive</div>' +
                        '</div>',

                    buttons: '<button class="w2ui-btn" onclick="ApkToolModule.textSearchAndReplacePerform();">Find & Replace</button>' +
                        '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                    width: 400,
                    height: 280,
                    overflow: 'hidden',
                    color: '#000',
                    speed: '0.3',
                    opacity: '0.4',
                    modal: true,
                    showClose: true,
                    //showMax: true,
                    onOpen: function (event) {
                        //console.log('open');
                    },
                    onClose: function (event) {
                        //console.log('close');
                    },
                    onKeydown: function (event) {
                    }
                });
                var $input_f = $("#input-fe-text-search-n-replace-f");
                var $input_r = $("#input-fe-text-search-n-replace-r");
                $input_r.keyup(function (event) {
                    var code = event.keyCode || event.which;
                    if (code == '13') { // enter button pressed
                        ApkToolModule.textSearchAndReplacePerform();
                    }
                });
                setTimeout(function () {
                    $input_f.focus();
                }, 300);
            },
            textSearchAndReplacePerform: function () {
                var searchQuery = $("#input-fe-text-search-n-replace-f").val();
                var replacement = $("#input-fe-text-search-n-replace-r").val();

                console.log("searching for: " + searchQuery + ", replace with: " + replacement);
                $.ajax({
                    url: "/api/protected/textSearchForEditor",
                    data: {action: "SEARCH_AND_REPLACE", searchQuery: searchQuery, replacement: replacement, caseSensitive: $('#check-fe-text-replace-sensitive').is(":checked"), projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                            w2ui['layout_file_editor'].show('bottom', window.instant);
                        }
                        w2ui['bottom_file_editor'].click('feBottomTabSearch');
                    },
                    success: function (data) {
                        w2popup.close();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        //hideBusysign();
                    }
                });
            },
            toolsAppIconModifierClicked: function () {
                $('#modalModifyAppIcon').modal('show');
            },
            toolsAppNameModifierClicked: function () {
                // ajax get old package name
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "ALL_AVAILABLE_APP_NAMES", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 30000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        //console.log(JSON.stringify(data));
                        if (data.appNames) {
                            var appNameModifierContainer = $('#app-name-modifier-container');
                            appNameModifierContainer.empty();
                            var template = $('#apkreverse-project-app-name-modifier-template').html();
                            var dataTmpl = {
                                tagName: function () {
                                    if (data.tagName)
                                        return data.tagName;
                                    else
                                        return "NaN";
                                },
                                declaration: data.declaration,
                                appNames: data.appNames
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            appNameModifierContainer.html(html);
                            $('#modalAppNameModifier').modal('show');
                        } else {
                            notify("Oops didn't find any app name!", "warning", 5000);
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsAppNameModifierPerform: function () {
                console.log("App Names Modifier submitted....");
                var newAppNames = [];
                var $li_app_name = $('li.li-app-name-modif');
                var isFormValid = true;
                var validationErrorMsg = "";
                if ($li_app_name.length > 0) {
                    $.each($li_app_name, function () {
                        var $li = $(this);
                        var language_code = $li.attr('data-appname-language');
                        var language_name = $li.find('span').text();
                        var old_app_name = $li.attr('data-appname-name');

                        var $new_app_name_input = $li.find('input.input-app-name-modif');
                        var new_app_name = $new_app_name_input.val();

                        // validate new Names Entries if empty and if respect Class naming convention
                        if (new_app_name === '') {
                            isFormValid = false;
                            validationErrorMsg = "Empty field for " + language_name;
                            $new_app_name_input.removeClass('border-unchanged-input');
                            $new_app_name_input.addClass('border-error-input');// error => red borders
                            return false;
                        } else {
                            if (new_app_name.trim() !== old_app_name) {
                                var newAppName = {
                                    language_code: language_code,
                                    old_app_name: old_app_name,
                                    new_app_name: new_app_name.trim()
                                };
                                newAppNames.push(newAppName);
                            }
                            $new_app_name_input.removeClass('border-error-input');
                            $new_app_name_input.addClass('border-unchanged-input');
                        }
                    });
                }

                if (isFormValid === false) {
                    notify(validationErrorMsg, 'error', 5000);
                    return;
                }

                if (newAppNames.length === 0) {
                    notify('Nothing to do because nothing has changed!', 'information', 5000);
                    return;
                }

                console.log('sending newAppNames: ' + JSON.stringify(newAppNames));

                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isAppNameModifierSubmitted === false) {
                    projectEditor.isAppNameModifierSubmitted = true;
                } else {
                    notify("App Name Modifier is already running, please wait!", "warning", 5000);
                    return;
                }

                // make an ajax call to start the process on server side
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {
                        action: "SUBMIT_APP_NAME_MODIFIER",
                        newAppNames: JSON.stringify(newAppNames),
                        declaration: $('#appname-modifier-declaration-input').val(),
                        tagName: $('#appname-modifier-tagName-input').val(),
                        projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')
                    },
                    method: 'POST',
                    timeout: 60000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data, textStatus, xhr) {
                        switch (xhr.status) {
                            case 204:
                            {
                                notify('Nothing to do because nothing has changed!', 'information', 5000);
                                break;
                            }
                            case 200:
                            {
                                // update opened files
                                console.log(JSON.stringify(data));
                                if (data.length > 0) {
                                    var tree = $("#jstree-tree-fileeditor");
                                    // check opened tabs if contains an updated node, if so => update its content
                                    if (w2ui['tabs_file_editor'].tabs.length > 0) {
                                        $.each(data, function (index) {
                                            var nodeId = data[index];
                                            var fileIsOpened = false;
                                            var listItems = $("ul#fe-main-list-tab-content > li");
                                            listItems.each(function (idx, li) {
                                                if ($(li).attr("data-node-id") === nodeId) {
                                                    fileIsOpened = true;
                                                    return false;
                                                }
                                            });
                                            if (fileIsOpened) { // tab already opened => update its content
                                                // get file content from server
                                                $.ajax({
                                                    url: "/api/protected/getFileContent",
                                                    data: {nodeId: nodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                                    method: 'GET',
                                                    dataType: "json",
                                                    timeout: 30000,
                                                    cache: false,
                                                    beforeSend: function () {
                                                        // disable editing while loading new content
                                                        var oldEditor = projectEditor.codeEditors[nodeId];
                                                        oldEditor.setOption("readOnly", true);
                                                    },
                                                    success: function (data) {
                                                        // get old editor scroll position
                                                        var oldEditor = projectEditor.codeEditors[nodeId];
                                                        var scroll_position = oldEditor.getScrollInfo();
                                                        var cursor_position = oldEditor.getCursor();
                                                        // update editor with new content
                                                        projectEditor.fileWrapper.updateTabContent(data);
                                                        var newEditor = projectEditor.codeEditors[nodeId];
                                                        // restore cursor and scroll positions
                                                        newEditor.scrollTo(scroll_position.left, scroll_position.top);
                                                        newEditor.setCursor({line: cursor_position.line, ch: 0});
                                                    },
                                                    error: function (xhr) {
                                                        var msg;
                                                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                            msg = xhr.statusText;
                                                        } else {
                                                            msg = xhr.statusText + ": " + xhr.responseText;
                                                        }
                                                        notify(msg, "error", 7000);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                    notify(data.length + " app name(s) modified with success", "success", 5000);
                                } else {
                                    notify("Oops nothing has changed!", "information", 5000);
                                }
                                // close the modal
                                $('#modalAppNameModifier').modal('hide');
                                break;
                            }
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isAppNameModifierSubmitted = false;
                    }
                });
            },
            toolsPackageNameChangerClicked: function () {
                // ajax get old package name
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "CURRENT_PACKAGE_NAME", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        if (!data.currentPackageName) {
                            notify("Can't find current package name inside AndroidManifest.xml file!", "error", 5000);
                            return;
                        }
                        w2popup.open({
                            title: 'Package name changer',
                            body: '<div class="w2ui-centered form-group">' +
                                '<label for="input-fe-current-package-name" style="margin-bottom: 10px;width: 100%;text-align: left;">Current package name:</label>' +
                                '<input type="text" class="form-control" maxlength="200" id="input-fe-current-package-name" style="background: #d1d7e6;" value="' + data.currentPackageName + '" readonly="readonly"/>' +
                                '<br>' +
                                '<label for="input-fe-new-package-name" style="margin-bottom: 10px;width: 100%;text-align: left;">New package name:</label>' +
                                '<input type="text" class="form-control" maxlength="200" id="input-fe-new-package-name" style="background: #d1d7e6;" placeholder="new package name here..." value=""/>' +
                                '<br>' +
                                '<div style="text-align: left;">' +
                                '<span style="word-break: break-all;font-size: 10px;color: #f97d7d;">DO NOT RELOAD THE PAGE OR EXIT THE APP while the package name changer is running, this will corrupt the project for sure.</span><br>' +
                                '<span style="word-break: break-all;font-size: 10px;">Please note that once the package name has been changed, the editor will reload the project.</span>' +
                                '</div>' +
                                '</div>',

                            buttons: '<button class="w2ui-btn" onclick="ApkToolModule.toolsPackageNameChangerPerform();">Change package name</button>' +
                                '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                            width: 480,
                            height: 320,
                            overflow: 'hidden',
                            color: '#000',
                            speed: '0.3',
                            opacity: '0.4',
                            modal: true,
                            showClose: true,
                            //showMax: true,
                            onOpen: function (event) {
                                //console.log('open');
                            },
                            onClose: function (event) {
                                //console.log('close');
                            },
                            onKeydown: function (event) {
                            }
                        });
                        var $input = $("#input-fe-new-package-name");

                        $input.keyup(function (event) {
                            var code = event.keyCode || event.which;
                            if (code == '13') { // enter button pressed
                                ApkToolModule.toolsPackageNameChangerPerform();
                            }
                        });
                        setTimeout(function () {
                            $input.focus();
                        }, 300);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsPackageNameChangerPerform: function () {
                var newPackageName = $('#input-fe-new-package-name').val().trim();
                console.log('updating package name to: ' + newPackageName);
                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isPackageNameChangerSubmitted === false) {
                    // validate package name
                    if (!newPackageName) {
                        notify("Empty package name error!", "error", 5000);
                        return;
                    } else {
                        var patternPackageName = /^(([a-z]{1}[a-z0-9\\d_]*\.)+[a-z][a-z0-9\\d_]*)$/m;
                        if (!newPackageName.match(patternPackageName)) {
                            notify('Incorrect new package name : ' + newPackageName, 'error', 2000);
                            return;
                        }
                    }
                    projectEditor.isPackageNameChangerSubmitted = true;
                } else {
                    notify("Package name changer is already running, please wait!", "warning", 5000);
                    return;
                }

                // make an ajax call to start the process of package name changer
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "SUBMIT_CHANGE_PACKAGE_NAME", newPackageName: newPackageName, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var tabProjectEditor = $('#tab-apkreverse-project-editor');

                        // minimize the editor
                        var toolbarMaxMinButton = w2ui['toolbar_file_editor']['items'][19];
                        toolbarMaxMinButton.checked = false;
                        toolbarMaxMinButton.text = 'Maximize';
                        tabProjectEditor.removeClass('maximized-div');
                        if (projectEditor.feHeight) {
                            $('#layout_file_editor').height(projectEditor.feHeight);
                        } else {
                            $('#layout_file_editor').height(410);
                        }
                        w2ui['toolbar_file_editor'].resize();
                        w2ui['layout_file_editor'].resize();

                        //close the w2ui popup
                        w2popup.close();
                        // show editor bottom log tab
                        try {
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                            w2ui['bottom_file_editor'].click('feBottomTabLog');
                        } catch (err) {
                            // do nothing
                        }
                        // block UI
                        tabProjectEditor.block({
                            message: "Changing package name. It may take few minutes...<br>Please wait and DO NOT REFRESH THE PAGE!",
                            centerY: false,
                            centerX: false,
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .8,
                                color: '#fff',
                                position: 'absolute',
                                margin: 'auto'
                            } });
                        setTimeout(function () {
                            $('.blockUI.blockOverlay').css('opacity', '0.4');
                        }, 500);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isPackageNameChangerSubmitted = false;
                    }
                });
            },
            toolsAppInfoUpdaterClicked: function () {
                // ajax get current app info: version and sdk
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "CURRENT_APP_INFO", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var template = $('#apkreverse-w2popup-appinfo-template').html();
                        var dataTemplate = {
                            data: data
                        };
                        var html = Mustache.to_html(template, dataTemplate);

                        w2popup.open({
                            title: 'Version/SDK Updater',
                            body: html,
                            buttons: '<button class="w2ui-btn" onclick="ApkToolModule.toolsAppInfoUpdaterPerform();">Update app info</button>' +
                                '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                            width: 450,
                            height: 420,
                            overflow: 'hidden',
                            color: '#000',
                            speed: '0.3',
                            opacity: '0.4',
                            modal: true,
                            showClose: true,
                            //showMax: true,
                            onOpen: function (event) {
                                //console.log('open');
                            },
                            onClose: function (event) {
                                //console.log('close');
                            },
                            onKeydown: function (event) {
                            }
                        });
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsAppInfoUpdaterPerform: function () {
                console.log("starting app info updater...");
                var ajax_data = {action: "SUBMIT_UPDATE_APP_INFO", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')};
                var $versionCode = $('#input-fe-new-version-code');
                var $versionName = $('#input-fe-new-version-name');
                var $minSdkVersion = $('#input-fe-new-sdk-min');
                var $targetSdkVersion = $('#input-fe-new-sdk-target');

                if ($versionCode.length) {
                    ajax_data.versionCode = $versionCode.val();
                }

                if ($versionName.length) {
                    ajax_data.versionName = $versionName.val();
                }

                if ($minSdkVersion.length) {
                    ajax_data.minSdkVersion = $minSdkVersion.val();
                }

                if ($targetSdkVersion.length) {
                    ajax_data.targetSdkVersion = $targetSdkVersion.val();
                }

                console.log('updating app info...');
                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isAppInfoUpdaterSubmitted === false) {
                    projectEditor.isAppInfoUpdaterSubmitted = true;
                } else {
                    notify("Version/SDK Updater is already running, please wait!", "warning", 5000);
                    return;
                }

                // make an ajax call to start the process of 'app info updater'
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: ajax_data,
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        w2popup.close();
                        notify("App Info updated with success", "success", 5000);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isAppInfoUpdaterSubmitted = false;
                    }
                });
            },
            toolsPackageRenamerClicked: function () {
                // ajax get all available packages for autocomplete
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "ALL_AVAILABLE_PACKAGES", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // ajax get old package name
                        w2popup.open({
                            title: 'Any Package Renamer',
                            body: '<div class="w2ui-centered form-group">' +
                                '<label for="input-fe-package-rename-src" style="margin-bottom: 10px;width: 100%;text-align: left;">Package name:</label>' +
                                '<input type="text" class="form-control" maxlength="250" id="input-fe-package-rename-src" style="background: #d1d7e6;" placeholder="enter an existing package name here..." value=""/>' +
                                '<br>' +
                                '<label for="input-fe-package-rename-dest" style="margin-bottom: 10px;width: 100%;text-align: left;">New package name:</label>' +
                                '<input type="text" class="form-control" maxlength="250" id="input-fe-package-rename-dest" style="background: #d1d7e6;" placeholder="rename that package here..." value=""/>' +
                                '<br>' +
                                '<div style="text-align: left;">' +
                                '<span style="word-break: break-all;font-size: 10px;color: #f97d7d;">DO NOT RELOAD THE PAGE OR EXIT THE APP while the package renamer is running, this will corrupt the project for sure.</span><br>' +
                                '<span style="word-break: break-all;font-size: 10px;">Please note that once package renaming is finished, the editor will reload the project.</span>' +
                                '</div>' +
                                '</div>',

                            buttons: '<button class="w2ui-btn" onclick="ApkToolModule.toolsPackageRenamerPerform();">Rename package</button>' +
                                '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                            width: 480,
                            height: 320,
                            overflow: 'hidden',
                            color: '#000',
                            speed: '0.3',
                            opacity: '0.4',
                            modal: true,
                            showClose: true,
                            //showMax: true,
                            onOpen: function (event) {
                                //console.log('opened popup Package Renamer');
                            },
                            onClose: function (event) {
                                //console.log('close');
                            },
                            onKeydown: function (event) {
                            }
                        });
                        var $input_src = $("#input-fe-package-rename-src");
                        var $input_dest = $("#input-fe-package-rename-dest");

                        $input_src.keyup(function (event) {
                            var code = event.keyCode || event.which;
                            if (code == '13') { // enter button pressed
                                ApkToolModule.toolsPackageRenamerPerform();
                            }
                        });

                        $input_dest.keyup(function (event) {
                            var code = event.keyCode || event.which;
                            if (code == '13') { // enter button pressed
                                ApkToolModule.toolsPackageRenamerPerform();
                            }
                        });

                        setTimeout(function () {
                            $input_src.focus();
                            $("#input-fe-package-rename-src").autocomplete({
                                source: data.availablePackages
                            });
                        }, 500);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsManifestEntriesRenamerClicked: function () {
                // ajax get all available packages for autocomplete
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "MANIFEST_ENTRIES", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        if (data) {
                            var response = {};

                            if (data.packageActivities) {
                                var packageActivities = [];
                                $.each(data.packageActivities, function (index) {
                                    var manifest_entry = data.packageActivities[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    packageActivities.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.packageActivities = packageActivities;
                            }
                            if (data.foreignActivities) {
                                var foreignActivities = [];
                                $.each(data.foreignActivities, function (index) {
                                    var manifest_entry = data.foreignActivities[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    foreignActivities.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.foreignActivities = foreignActivities;
                            }
                            if (data.packageServices) {
                                var packageServices = [];
                                $.each(data.packageServices, function (index) {
                                    var manifest_entry = data.packageServices[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    packageServices.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.packageServices = packageServices;
                            }
                            if (data.foreignServices) {
                                var foreignServices = [];
                                $.each(data.foreignServices, function (index) {
                                    var manifest_entry = data.foreignServices[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    foreignServices.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.foreignServices = foreignServices;
                            }
                            if (data.packageReceivers) {
                                var packageReceivers = [];
                                $.each(data.packageReceivers, function (index) {
                                    var manifest_entry = data.packageReceivers[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    packageReceivers.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.packageReceivers = packageReceivers;
                            }
                            if (data.foreignReceivers) {
                                var foreignReceivers = [];
                                $.each(data.foreignReceivers, function (index) {
                                    var manifest_entry = data.foreignReceivers[index];
                                    var split = manifest_entry.split('.');
                                    var class_name = split[split.length - 1];
                                    split.pop();
                                    var packageName = split.join('.');
                                    foreignReceivers.push({manifest_entry: manifest_entry, packageName: packageName, class_name: class_name});
                                });
                                response.foreignReceivers = foreignReceivers;
                            }

                            var manifEntriesRenamerContainer = $('#manifest-entries-container');
                            manifEntriesRenamerContainer.empty()
                            var template = $('#apkreverse-project-manifest-rentries-renamer-template').html();
                            var dataTmpl = {
                                response: response
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            manifEntriesRenamerContainer.html(html);
                            $('#modalManifestEntriesRenamer').modal('show');
                        } else {
                            notify("Oops didn't find manifest entry!", "warning", 5000);
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsManifestEntriesRenamerPerform: function () {
                console.log("Manifest Entries Transformer submitteed....");
                var customNames = {};
                // build custom names
                var $li_manifest_entries = $('li.li-manifest-entry-ren');

                var isFormValid = true;
                var validationErrorMsg = "";
                if ($li_manifest_entries.length > 0) {
                    var patternClassName = /^[A-Za-z]{1}[A-Za-z0-9\\d_]*\s*$/g;

                    $.each($li_manifest_entries, function () {
                        var $li = $(this);
                        var manifest_entry = $li.attr('data-manifest-entry');

                        var manifest_entry_split = manifest_entry.split('.');
                        var old_class_name = manifest_entry_split[manifest_entry_split.length - 1];

                        var $new_class_name_input = $li.find('input.input-manifest-entry-ren');
                        var new_class_name = $new_class_name_input.val();

                        // validate new Names Entries if empty and if respect Class naming convention
                        if (new_class_name === '') {
                            isFormValid = false;
                            validationErrorMsg = "Empty field for '" + manifest_entry + "'";
                            $new_class_name_input.removeClass('border-unchanged-input');
                            $new_class_name_input.addClass('border-error-input');
                            return false;
                        } else {
                            if (new_class_name !== old_class_name) {
                                if (!new_class_name.match(patternClassName)) {// bad class name
                                    isFormValid = false;
                                    validationErrorMsg = "'" + new_class_name + "' is not a valid class name";
                                    $new_class_name_input.removeClass('border-unchanged-input');
                                    $new_class_name_input.addClass('border-error-input');
                                    return false;
                                } else {
                                    customNames[manifest_entry] = new_class_name.trim();
                                    $new_class_name_input.removeClass('border-error-input');
                                    $new_class_name_input.addClass('border-unchanged-input');
                                }
                            } else {
                                $new_class_name_input.removeClass('border-error-input');
                                $new_class_name_input.addClass('border-unchanged-input');
                            }
                        }
                    });
                }

                if (isFormValid === false) {
                    notify(validationErrorMsg, 'error', 5000);
                    return;
                }

                if (Object.keys(customNames).length === 0) {
                    notify('Nothing to do because nothing has changed!', 'information', 5000);
                    return;
                }


                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isManifestEntriesRenamerSubmitted === false) {
                    projectEditor.isManifestEntriesRenamerSubmitted = true;
                } else {
                    notify("Manifest Entries Transformer is already running, please wait!", "warning", 5000);
                    return;
                }

                // make an ajax call to start the process of package name changer
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "SUBMIT_MANIFEST_ENTRIES_RENAMER", customNames: JSON.stringify(customNames),
                        projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data, textStatus, xhr) {
                        switch (xhr.status) {
                            case 204:
                            {
                                notify('Nothing to do because nothing has changed!', 'information', 5000);
                                break;
                            }
                            case 200:
                            {
                                var tabProjectEditor = $('#tab-apkreverse-project-editor');
                                // minimize the editor
                                var toolbarMaxMinButton = w2ui['toolbar_file_editor']['items'][19];
                                toolbarMaxMinButton.checked = false;
                                toolbarMaxMinButton.text = 'Maximize';
                                tabProjectEditor.removeClass('maximized-div');
                                if (projectEditor.feHeight) {
                                    $('#layout_file_editor').height(projectEditor.feHeight);
                                } else {
                                    $('#layout_file_editor').height(410);
                                }
                                w2ui['toolbar_file_editor'].resize();
                                w2ui['layout_file_editor'].resize();

                                //close and clean the modal
                                $('#modalManifestEntriesRenamer').modal('hide');
                                $('#manifest-entries-container').empty();

                                // show editor bottom log tab
                                try {
                                    if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                        w2ui['layout_file_editor'].show('bottom', window.instant);
                                    }
                                    w2ui['bottom_file_editor'].click('feBottomTabLog');
                                } catch (err) {
                                    // do nothing
                                }
                                // block UI
                                tabProjectEditor.block({
                                    message: "Renaming manifest entries. It may take few minutes...<br>Please wait and DO NOT REFRESH THE PAGE!",
                                    centerY: false,
                                    centerX: false,
                                    css: {
                                        border: 'none',
                                        padding: '15px',
                                        backgroundColor: '#000',
                                        '-webkit-border-radius': '10px',
                                        '-moz-border-radius': '10px',
                                        opacity: .8,
                                        color: '#fff',
                                        position: 'absolute',
                                        margin: 'auto'
                                    } });
                                setTimeout(function () {
                                    $('.blockUI.blockOverlay').css('opacity', '0.4');
                                }, 500);
                                // websocket will take care of the rest
                                break;
                            }
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isManifestEntriesRenamerSubmitted = false;
                    }
                });
            },
            toolsPackageRenamerPerform: function () {
                var existingPackageName = $('#input-fe-package-rename-src').val().trim();
                var packageNewName = $('#input-fe-package-rename-dest').val().trim();
                console.log("Renaming package '" + existingPackageName + "' to '" + packageNewName + "'");


                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isPackageRenamerSubmitted === false) {
                    // validate package name
                    if (!existingPackageName) {
                        notify("Existing package name is empty!", "error", 5000);
                        return;
                    }
                    if (!packageNewName) {
                        notify("New package name is empty!", "error", 5000);
                        return;
                    }

                    var patternPackageName = /^(([a-z]{1}[a-z0-9\\d_]*\.)+[a-z][a-z0-9\\d_]*)$/m;
                    if (!packageNewName.match(patternPackageName)) {
                        notify('Incorrect new package name : ' + packageNewName, 'error', 2000);
                        return;
                    }
                    projectEditor.isPackageRenamerSubmitted = true;
                } else {
                    notify("Package renamer is already running, please wait!", "warning", 5000);
                    return;
                }

                // make an ajax call to start the process of package name changer
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "SUBMIT_RENAME_PACKAGE", existingPackageName: existingPackageName.trim(), packageNewName: packageNewName.trim(),
                        projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var tabProjectEditor = $('#tab-apkreverse-project-editor');

                        // minimize the editor
                        var toolbarMaxMinButton = w2ui['toolbar_file_editor']['items'][19];
                        toolbarMaxMinButton.checked = false;
                        toolbarMaxMinButton.text = 'Maximize';
                        tabProjectEditor.removeClass('maximized-div');
                        if (projectEditor.feHeight) {
                            $('#layout_file_editor').height(projectEditor.feHeight);
                        } else {
                            $('#layout_file_editor').height(410);
                        }
                        w2ui['toolbar_file_editor'].resize();
                        w2ui['layout_file_editor'].resize();

                        //close the w2ui popup
                        w2popup.close();
                        // show editor bottom log tab
                        try {
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                            w2ui['bottom_file_editor'].click('feBottomTabLog');
                        } catch (err) {
                            // do nothing
                        }
                        // block UI
                        tabProjectEditor.block({
                            message: "Renaming package. It may take few minutes...<br>Please wait and DO NOT REFRESH THE PAGE!",
                            centerY: false,
                            centerX: false,
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .8,
                                color: '#fff',
                                position: 'absolute',
                                margin: 'auto'
                            } });
                        setTimeout(function () {
                            $('.blockUI.blockOverlay').css('opacity', '0.4');
                        }, 500);
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isPackageRenamerSubmitted = false;
                    }
                });
            },
            toolsBuildApkForDebugClicked: function () {
                w2confirm('Do want to build a DEBUG APK?')
                    .yes(function () {
                        projectEditor.toolsBuildApkForDebugPerform();
                    })
                    .no(function () {
                    });
            },
            toolsBuildApkForDebugPerform: function () {
                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isBuildDebugApkSubmitted === false) {
                    projectEditor.isBuildDebugApkSubmitted = true;
                } else {
                    notify("BUILD DEBUG APK is already running, please wait!", "warning", 5000);
                    return;
                }
                console.log('Building DEBUG apk...');
                // make an ajax call to start the process of building debug apk
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "SUBMIT_BUILD_APK_DEBUG", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // show editor bottom log tab
                        try {
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                            w2ui['bottom_file_editor'].click('feBottomTabLog');
                        } catch (err) {
                            // do nothing
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isBuildDebugApkSubmitted = false;
                    }
                });
            },
            toolsBuildApkForReleaseClicked: function () {
                // ajax get all available keystores
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "GET_INFO", infoType: "ALL_AVAILABLE_KEYSTORES", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 20000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // if no keystore found => ask the user to create a new keystore
                        if (data.keystores.length === 0) {
                            w2confirm('You must create a keystore before building RELEASE apk.<br>' +
                                'Do you want to create a keystore?')
                                .yes(function () {
                                    $('ul#apkreverse-main-navtab a[href="#apktools-keytool-data"]').tab('show');
                                })
                                .no(function () {

                                });
                        } else {
                            var template = $('#apkreverse-w2popup-release-apk-template').html();
                            var dataTemplate = {
                                keystores: data.keystores
                            };
                            var html = Mustache.to_html(template, dataTemplate);

                            w2popup.open({
                                title: 'Build RELEASE apk',
                                body: html,

                                buttons: '<button class="w2ui-btn" onclick="ApkToolModule.toolsBuildApkForReleasePerform();">Build RELEASE apk</button>' +
                                    '<button class="w2ui-btn" onclick="w2popup.close();">Cancel</button> ',
                                width: 480,
                                height: 320,
                                overflow: 'hidden',
                                color: '#000',
                                speed: '0.3',
                                opacity: '0.4',
                                modal: true,
                                showClose: true,
                                //showMax: true,
                                onOpen: function (event) {
                                    //console.log('opened popup Package Renamer');
                                },
                                onClose: function (event) {
                                    //console.log('close');
                                },
                                onKeydown: function (event) {
                                }
                            });

                            var $input = $("#input-fe-release-apk-name");
                            var $apkNameOutput = $("#input-fe-release-apk-nameoutput");

                            $input.keyup(function (event) {
                                var code = event.keyCode || event.which;
                                if (code == '13') { // enter button pressed
                                    ApkToolModule.toolsBuildApkForReleasePerform();
                                } else {
                                    var name = $input.val();
                                    if (name !== '') {
                                        $apkNameOutput.text($input.val() + '-aligned-signed.apk');
                                    } else {
                                        $apkNameOutput.text('');
                                    }
                                }
                            });

                            setTimeout(function () {
                                $input.focus();
                            }, 500);
                        }
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                    }
                });
            },
            toolsBuildApkForReleasePerform: function () {
                var apkName = $("#input-fe-release-apk-name").val();
                var keystoreUuid = $('#input-fe-release-apk-keystores').val();

                if (!apkName) {
                    notify("Please enter an apk name!", "error", 5000);
                }

                if (!keystoreUuid) {
                    notify("Please select a keystore!", "error", 5000);
                }


                // VERY IMPORTANT prevent double submit by hitting 'enter' key or submit button, using a global variable
                if (projectEditor.isBuildReleaseApkSubmitted === false) {
                    projectEditor.isBuildReleaseApkSubmitted = true;
                } else {
                    notify("BUILD RELEASE APK is already running, please wait!", "warning", 5000);
                    return;
                }
                console.log('Building RELEASE apk...');
                // make an ajax call to start the process of building release apk
                $.ajax({
                    url: "/api/protected/toolsOfEditor",
                    data: {action: "SUBMIT_BUILD_APK_RELEASE", apkName: apkName, keystoreUuid: keystoreUuid, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                    method: 'POST',
                    dataType: "json",
                    timeout: 15000,
                    cache: false,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        // show editor bottom log tab
                        try {
                            if ($('#layout_layout_file_editor_panel_bottom').css('display') === 'none') {
                                w2ui['layout_file_editor'].show('bottom', window.instant);
                            }
                            w2ui['bottom_file_editor'].click('feBottomTabLog');
                        } catch (err) {
                            // do nothing
                        }
                        w2popup.close();
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    },
                    complete: function () {
                        hideBusysign();
                        projectEditor.isBuildReleaseApkSubmitted = false;
                    }
                });
            },
            bottomTabsManager: {
                getSearchResult: function (processId) {
                    // ajax get jsTree result
                    $.ajax({
                        url: "/api/protected/textSearchForEditor",
                        data: {action: "GET_RESULT_TEXT_SEARCH", processId: processId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                        method: 'POST',
                        dataType: "json",
                        timeout: 20000,
                        cache: false,
                        success: function (data) {
                            if (data.length === 0) { // check if empty array => search with zero result
                                return;
                            }
                            // build js tree
                            var randomJsTreeId = generateUUID();
                            // add jstree to result
                            var $div_search_cnt = $('#feBottomTabSearch-content');
                            $div_search_cnt.append('<div><div id="' + randomJsTreeId + '" class="fileeditor-search-tree"></div></div>');

                            $('#' + randomJsTreeId).jstree({
                                plugins: ['themes', 'types', 'wholerow'],
                                core: {
                                    themes: {
                                        name: "default-dark",
                                        dots: true,
                                        icons: false
                                    },
                                    data: data
                                },
                                types: {
                                    root: {},
                                    file: {},
                                    line: {}
                                }
                            }).bind("dblclick.jstree", function (event) {
                                // on double click on file => open it in a tab
                                var searchTree = $(this).jstree();
                                var node = searchTree.get_node(event.target);

                                //console.log("node.type: " + node.type);
                                if (node.type === 'line') {
                                    console.log("open file id: " + node.data.fileNodeId + " and set cursor to line: " + node.data.lineNumber);
                                    /*
                                     Algorithm:
                                     1- check if node's editor tab is already opened
                                     2- if:  already opened => set it as active and select its node and put cursor at line number
                                     3- else: make an ajax call to get list of parents using graph path (same as jsTree search)
                                     4- on success => call tree.load_node([arrra_parents], callback) with callback = 'set tab as active'+'select its node'+ 'put cursor at line number'
                                     */

                                    var nodeId = node.data.fileNodeId;
                                    var lineNumber = parseInt(node.data.lineNumber);
                                    var startChar = parseInt(node.data.start);
                                    var endChar = parseInt(node.data.end);
                                    // check if its is opened => set it as active and set its jsTree node as selected and put cursor at line number
                                    var foundTab = false;
                                    var listItems = $("ul#fe-main-list-tab-content > li");
                                    listItems.each(function (idx, li) {
                                        if ($(li).attr("data-node-id") === node.id) {
                                            foundTab = true;
                                            return false;
                                        }
                                    });

                                    if (foundTab) { // tab already opened => set as active
                                        w2ui['tabs_file_editor'].select(node.id);
                                        console.log("tab already opened  => : id:" + node.id);
                                        listItems.each(function (idx, li) {
                                            if ($(li).attr("data-node-id") === node.id) {
                                                $(li).removeClass('hidden');
                                            } else {
                                                $(li).removeClass('hidden').addClass('hidden');
                                            }
                                        });

                                        // set cursor at lineNumber
                                        var nodeToOpen = $('#jstree-tree-fileeditor').jstree(true).get_node(nodeId);
                                        projectEditor.tabsManager.openTab(nodeToOpen, $('#project-editor-devenvironment-data').attr('data-project-uuid'), function () {
                                            var tree = $('#jstree-tree-fileeditor');
                                            // set corresponding jstree node as selected
                                            tree.jstree("deselect_all");
                                            tree.jstree('select_node', nodeToOpen.id);
                                            // bring focus to corresponding jstree node with a scroll animation
                                            var nodeLine = tree.jstree(true).get_node(nodeToOpen.id, true).children('.jstree-anchor');
                                            var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                                            if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                                                nodeLine.focus();
                                            } else { // if not not visible inside viewport => scroll with animation
                                                container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                                    nodeLine.focus();
                                                });
                                            }
                                            // codemirror editor focus lineNumber
                                            var cmInstance = projectEditor.codeEditors[nodeToOpen.id];
                                            if (cmInstance) {
                                                cmInstance.focus();
                                                cmInstance.setCursor({line: lineNumber - 1, ch: startChar});

                                                // clear all markers
                                                for (var l = 0; l < projectEditor.cmMarkers.length; l++) {
                                                    projectEditor.cmMarkers[l].clear();
                                                }
                                                projectEditor.cmMarkers.length = 0;

                                                // check if text contains the search term before applying the marker (using case sensibility)
                                                var line = cmInstance.getDoc().getLine(lineNumber - 1);
                                                var line_search_term = line.substring(startChar, endChar);
                                                var search_term = searchTree.get_node(0).data.search_term;
                                                var is_case_sensitive = searchTree.get_node(0).data.case_sensitive;
                                                if (is_case_sensitive === true) {
                                                    if (line_search_term !== search_term) {
                                                        return;
                                                    }
                                                } else {
                                                    if (line_search_term.toLocaleLowerCase() !== search_term.toLocaleLowerCase()) {
                                                        return;
                                                    }
                                                }

                                                // add new marker
                                                var marker = cmInstance.markText({
                                                    line: lineNumber - 1,
                                                    ch: startChar
                                                }, {
                                                    line: lineNumber - 1,
                                                    ch: endChar
                                                }, {
                                                    className: "cm-marker-search"
                                                });
                                                projectEditor.cmMarkers.push(marker);
                                            }
                                        });


                                    } else { // not loaded => open it in a new tab and set tab as active
                                        //ajax call LOAD_NODE_DATA
                                        $.ajax({
                                            url: "/api/protected/textSearchForEditor",
                                            data: {action: "LOAD_NODE_DATA", nodeId: nodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                            method: 'POST',
                                            dataType: "json",
                                            timeout: 20000,
                                            cache: false,
                                            success: function (data) {
                                                // jstree load nodes with callback
                                                // callback let us select the jsTree node and bring focus to it with  animation
                                                $('#jstree-tree-fileeditor').jstree(true).load_node(data.parentsArray, function () {
                                                    var node = $('#jstree-tree-fileeditor').jstree(true).get_node(data.nodeId);
                                                    projectEditor.tabsManager.openTab(node, $('#project-editor-devenvironment-data').attr('data-project-uuid'), function () {
                                                        var tree = $('#jstree-tree-fileeditor');
                                                        // set corresponding jstree node as selected
                                                        tree.jstree("deselect_all");
                                                        tree.jstree('select_node', node.id);
                                                        // bring focus to corresponding jstree node with a scroll animation
                                                        var nodeLine = tree.jstree(true).get_node(node.id, true).children('.jstree-anchor');
                                                        var container = $('#layout_layout_file_editor_panel_left .w2ui-panel-content');
                                                        if (nodeLine.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                                                            nodeLine.focus();
                                                        } else { // if not not visible inside viewport => scroll with animation
                                                            container.animate({scrollTop: nodeLine.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                                                nodeLine.focus();
                                                            });
                                                        }
                                                        console.log("bringing focus to line : " + lineNumber);
                                                        // codemirror editor focus lineNumber
                                                        var cmInstance = projectEditor.codeEditors[node.id];
                                                        if (cmInstance) {
                                                            cmInstance.focus();
                                                            cmInstance.setCursor({line: lineNumber - 1, ch: startChar});

                                                            // clear all markers
                                                            for (var l = 0; l < projectEditor.cmMarkers.length; l++) {
                                                                projectEditor.cmMarkers[l].clear();
                                                            }
                                                            projectEditor.cmMarkers.length = 0;

                                                            // check if text contains the search term before applying the marker
                                                            var line = cmInstance.getDoc().getLine(lineNumber - 1);
                                                            var line_search_term = line.substring(startChar, endChar);
                                                            var search_term = searchTree.get_node(0).data.search_term;
                                                            var is_case_sensitive = searchTree.get_node(0).data.case_sensitive;
                                                            if (is_case_sensitive === true) {
                                                                if (line_search_term !== search_term) {
                                                                    console.log("CASE_SENSIBLE oops line_search_term: " + line_search_term + " is different from: " + search_term);
                                                                    return;
                                                                }
                                                            } else {
                                                                if (line_search_term.toLocaleLowerCase() !== search_term.toLocaleLowerCase()) {
                                                                    console.log("oops line_search_term: " + line_search_term + " is different from: " + search_term);
                                                                    return;
                                                                }
                                                            }

                                                            // add new marker
                                                            var marker = cmInstance.markText({
                                                                line: lineNumber - 1,
                                                                ch: startChar
                                                            }, {
                                                                line: lineNumber - 1,
                                                                ch: endChar
                                                            }, {
                                                                className: "cm-marker-search"
                                                            });
                                                            projectEditor.cmMarkers.push(marker);
                                                        }
                                                    });
                                                });
                                            },
                                            error: function (xhr) {
                                                var msg;
                                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                    msg = xhr.statusText;
                                                } else {
                                                    msg = xhr.statusText + ": " + xhr.responseText;
                                                }
                                                notify(msg, "error", 7000);
                                            }
                                        });
                                    }
                                }
                            });
                            // save scroll position
                            var scrollPosition = $div_search_cnt.height();
                            $('ul#fe-bottom-list-tab-content').scrollTop(scrollPosition);// scroll to bottom => new content always visible
                            $div_search_cnt.attr('data-scroll', scrollPosition);

                            try {
                                w2ui['bottom_file_editor'].click('feBottomTabSearch');
                            } catch (err) {
                                // do nothing
                            }

                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        }
                    });
                },
                getSearchAndReplaceResult: function (processId) {
                    // ajax get jsTree result
                    $.ajax({
                        url: "/api/protected/textSearchForEditor",
                        data: {action: "GET_RESULT_TEXT_SEARCH_AND_REPLACE", processId: processId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                        method: 'POST',
                        dataType: "json",
                        timeout: 20000,
                        cache: false,
                        success: function (data) {
                            if (data.nb_files === 0) { // check if search with zero result => nothing to do just return
                                return;
                            } else {
                                if (data.modified_file_ids) {// some files has been modified => check if these files are opened in the editor, if so => update their content
                                    var tree = $('#jstree-tree-fileeditor').jstree();
                                    for (var i = 0; i < data.modified_file_ids.length; i++) {
                                        var nodeId = data.modified_file_ids[i];
                                        // check if file having nodeId is opened
                                        var fileIsOpened = false;
                                        var listItems = $("ul#fe-main-list-tab-content > li");
                                        listItems.each(function (idx, li) {
                                            if ($(li).attr("data-node-id") === nodeId) {
                                                fileIsOpened = true;
                                                return false;
                                            }
                                        });

                                        if (fileIsOpened) { // tab already opened => update its content
                                            var node = tree.get_node(nodeId);
                                            console.log("Search and replace ==== modified file [" + node.text + "] is opened");
                                            // get file content from server
                                            $.ajax({
                                                url: "/api/protected/getFileContent",
                                                data: {nodeId: nodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                                method: 'GET',
                                                dataType: "json",
                                                timeout: 30000,
                                                cache: false,
                                                beforeSend: function () {
                                                    // disable editing while loading new content
                                                    var oldEditor = projectEditor.codeEditors[node.id];
                                                    oldEditor.setOption("readOnly", true);
                                                },
                                                success: function (data) {
                                                    // get old editor scroll position
                                                    var oldEditor = projectEditor.codeEditors[node.id];
                                                    var scroll_position = oldEditor.getScrollInfo();
                                                    var cursor_position = oldEditor.getCursor();
                                                    // update editor with new content
                                                    projectEditor.fileWrapper.updateTabContent(data);
                                                    var newEditor = projectEditor.codeEditors[node.id];
                                                    // restore cursor and scroll positions
                                                    newEditor.scrollTo(scroll_position.left, scroll_position.top);
                                                    newEditor.setCursor({line: cursor_position.line, ch: 0});

                                                },
                                                error: function (xhr) {
                                                    var msg;
                                                    if (xhr.responseText === "undefined" || !xhr.responseText) {
                                                        msg = xhr.statusText;
                                                    } else {
                                                        msg = xhr.statusText + ": " + xhr.responseText;
                                                    }
                                                    notify(msg, "error", 7000);
                                                }
                                            });
                                        }
                                    }
                                }
                            }

                            // save scroll position
                            var $div_search_cnt = $('#feBottomTabSearch-content');
                            var scrollPosition = $div_search_cnt.height();
                            $('ul#fe-bottom-list-tab-content').scrollTop(scrollPosition);// scroll to bottom => new content always visible
                            $div_search_cnt.attr('data-scroll', scrollPosition);

                            try {
                                w2ui['bottom_file_editor'].click('feBottomTabSearch');
                            } catch (err) {
                                // do nothing
                            }
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        }
                    });
                },
                reloadApkFilesTab: function () {
                    console.log('Reloading APK files Tab content');
                    // ajax call get all apk files of the current project and populate the Tab content
                    $.ajax({
                        url: "/api/protected/toolsOfEditor",
                        data: {action: "GET_INFO", infoType: "ALL_AVAILABLE_APKS", projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                        method: 'POST',
                        dataType: "json",
                        timeout: 15000,
                        cache: false,
                        success: function (data) {
                            // populate the template
                            var templateApkFilesInfo = $('#apkreverse-project-apkfiles-info-template').html();
                            var dataTemplateApkFilesInfo = {
                                project: data
                            };
                            var htmlApkFilesInfo = Mustache.to_html(templateApkFilesInfo, dataTemplateApkFilesInfo);
                            $('#feBottomTabApkFiles-content').html(htmlApkFilesInfo);

                            // show editor bottom tab 'apk files'
                            try {
                                w2ui['bottom_file_editor'].click('feBottomTabApkFiles');
                            } catch (err) {
                                // do nothing
                            }
                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        }
                    });
                },
                apkFile: {
                    feApkTabDownload: function (e, apkFileName, apkType) {
                        e.preventDefault();
                        console.log('Downloading APK file :' + apkFileName);
                        try {
                            // remove previous iframe
                            var previousDownloadFrame = document.getElementById('iframe-fe-file-download');
                            if (previousDownloadFrame !== null) {
                                previousDownloadFrame.parentNode.removeChild(previousDownloadFrame);
                            }
                            // add new iframe
                            var downloadFrame = document.createElement("iframe");
                            downloadFrame.id = 'iframe-fe-file-download';
                            downloadFrame.setAttribute('class', "downloadFrameScreen");
                            var url = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port
                                + "/api/protected/downloadApkFile?projectUuid=" + $('#project-editor-devenvironment-data').attr('data-project-uuid') + "&apkName=" + apkFileName + "&apkType=" + apkType;
                            downloadFrame.setAttribute('src', url);
                            document.body.appendChild(downloadFrame);
                        } catch (err) {
                            notify(err.toString(), "error", 7000);
                        }
                    },
                    feApkTabRemove: function (e, apkFileName) {
                        e.preventDefault();
                        w2confirm('Do want to remove apk "' + apkFileName + '"?')
                            .yes(function () {
                                // ajax remove and refresh apk list on success
                                $.ajax({
                                    url: "/api/protected/toolsOfEditor",
                                    data: {action: "REMOVE_APK", apkFileName: apkFileName, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                                    method: 'POST',
                                    dataType: "json",
                                    timeout: 15000,
                                    cache: false,
                                    success: function (data) {
                                        notify("Apk file '" + apkFileName + "' removed with success", "success", 5000);
                                        projectEditor.bottomTabsManager.reloadApkFilesTab();
                                    },
                                    error: function (xhr) {
                                        var msg;
                                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                                            msg = xhr.statusText;
                                        } else {
                                            msg = xhr.statusText + ": " + xhr.responseText;
                                        }
                                        notify(msg, "error", 7000);
                                    }
                                });
                            })
                            .no(function () {
                            });
                    },
                    feApkTabInfo: function (e, apkFileName, apkType) {
                        e.preventDefault();
                        // ajax remove and refresh apk list on success
                        $.ajax({
                            url: "/api/protected/toolsOfEditor",
                            data: {
                                action: "GET_APK_INFO", apkFileName: apkFileName,
                                projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'),
                                apkType: apkType
                            },
                            method: 'POST',
                            dataType: "json",
                            timeout: 20000,
                            cache: false,
                            beforeSend: function () {
                                showBusysign();
                            },
                            success: function (data) {
                                // populate apk info template
                                var template = $('#apkreverse-apk-info-result-template').html();
                                var dataTemplate = {
                                    response: data,
                                    apk_file_name: apkFileName
                                };
                                var html = Mustache.to_html(template, dataTemplate);
                                $("div#apk-info-container").html(html);

                                // apk info js-tree init
                                $('#jstree-tree-apk-info')
                                    .jstree({
                                        core: {
                                            themes: {
                                                name: "default",
                                                dots: true,
                                                icons: true,
                                                url: "/static/public/plugins/jstree/themes/default/style.min.css"
                                            },
                                            data: jsTreeWrapper.convertToHierarchy(data.assets)
                                        },
                                        plugins: ['themes', 'html_data', 'types', 'wholerow', 'search', 'sort'],
                                        types: {
                                            folder: {
                                                icon: '/static/public/plugins/jstree/b_folder.png'
                                            },

                                            file: {
                                                icon: '/static/public/plugins/jstree/a_file.png'
                                            }
                                        },
                                        sort: function (a, b) { // sort by icon and after by text
                                            var a1 = this.get_node(a);
                                            var b1 = this.get_node(b);
                                            if (a1.icon == b1.icon) {
                                                return (a1.text > b1.text) ? 1 : -1;
                                            } else {
                                                return (a1.icon > b1.icon) ? 1 : -1;
                                            }
                                        },
                                        search: {
                                            case_insensitive: true,
                                            show_only_matches: true
                                        }
                                    });

                                var filterJsTreeApkInfoSearch = function (textFilter) { // Js Tree filter search
                                    var searchResult = $('#jstree-tree-apk-info').jstree('search', textFilter);
                                    var searchCount = $(searchResult).find('.jstree-search').length;
                                    var spanFilter = $('span#filter-counter-js-tree-apk-info');
                                    if (searchCount > 0) {
                                        spanFilter.text(searchCount.toString());
                                        spanFilter.show();
                                    } else {
                                        spanFilter.text('0');
                                        spanFilter.hide();
                                    }
                                };

                                var filterJsTreeApkInfoDebounce = debounce(function (input, e) { // Js Tree filter search debounce ==> delay filtering
                                    var code = e.keyCode || e.which;
                                    // tab=9, shift=16, ctrl=17 , arrows =37..40
                                    if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                                    if (code == '27') $(input).val('');
                                    // same word in all filters inputs
                                    filterJsTreeApkInfoSearch($(input).val());
                                }, 250);

                                // js-tree search plugin init
                                $("#js-search-apk-info-input").keyup(function (e) {
                                    filterJsTreeApkInfoDebounce(this, e);
                                });
                                // show modal
                                $('#modalApkInfo').modal('show');
                            },
                            error: function (xhr) {
                                var msg;
                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                    msg = xhr.statusText;
                                } else {
                                    msg = xhr.statusText + ": " + xhr.responseText;
                                }
                                notify(msg, "error", 7000);
                            }, complete: function () {
                                hideBusysign();
                            }
                        });
                    },
                    feApkTabInstall: function (e, apkFileName, apkType) {
                        e.preventDefault();
                        // ajax install apk file
                        $.ajax({
                            url: "/api/protected/toolsOfEditor",
                            data: {action: "INSTALL_APK_DEVICE", apkFileName: apkFileName, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid'), apkType: apkType},
                            method: 'POST',
                            dataType: "json",
                            timeout: 20000,
                            cache: false,
                            beforeSend: function () {
                                //showBusysign();
                            },
                            success: function (data) {
                                // show editor bottom tab 'Logs'
                                try {
                                    w2ui['bottom_file_editor'].click('feBottomTabLog');
                                } catch (err) {
                                    // do nothing
                                }
                            },
                            error: function (xhr) {
                                var msg;
                                if (xhr.responseText === "undefined" || !xhr.responseText) {
                                    msg = xhr.statusText;
                                } else {
                                    msg = xhr.statusText + ": " + xhr.responseText;
                                }
                                notify(msg, "error", 7000);
                            }, complete: function () {
                                //hideBusysign();
                            }
                        });

                    },
                    feApkTabNewProject: function (e, apkFileName) {
                        e.preventDefault();
                        w2confirm('Do you want create a new project from this apk "' + apkFileName + '"?')
                            .yes(function () {

                            })
                            .no(function () {
                            });
                    }
                }
            },
            folderExplorer: {
                last_opened_node: null,
                last_positive_filter: null,
                last_negative_filter: null,
                resetStateOnCloseProject: function () {
                    projectEditor.folderExplorer.last_opened_node = null;
                    projectEditor.folderExplorer.last_positive_filter = null;
                    projectEditor.folderExplorer.last_negative_filter = null;
                },
                cleanExplorerModal: function () {
                    $('#folder-explorer-current-path').text('');
                    $('#folder-explorer-up').attr('data-up-folder', '');

                    $('#filter-positive-folder-exp-input').val('');
                    $('#filter-negative-folder-exp-input').val('');
                    var $folderFilterCounters = $('.exp-filter-counter');
                    $folderFilterCounters.text('');
                    $folderFilterCounters.hide();

                    $('#folder-explorer-container').html('');
                },

                // positive filter
                filterExplorer: function () {
                    var $container = $('div#folder-explorer-container');
                    var $elements = $container.find('div.exp-img-cont');
                    var filterPositiveSplits = [];
                    var filterNegativeSplits = [];

                    var textPositiveFilter = $('#filter-positive-folder-exp-input').val();
                    var textNegativeFilter = $('#filter-negative-folder-exp-input').val();

                    if (textPositiveFilter !== '' || textNegativeFilter !== '') {
                        filterPositiveSplits = textPositiveFilter.split(',');
                        filterNegativeSplits = textNegativeFilter.split(',');

                        $elements.show().filter(function () {
                            var text = $(this).text().replace(/\s+/g, ' ').toLowerCase(); // replace many spaces by one space

                            var positive_return = false;
                            for (var i = 0; i < filterPositiveSplits.length; i++) {
                                if (filterPositiveSplits[i] !== '' && text.indexOf($.trim(filterPositiveSplits[i]).replace(/ +/g, ' ').toLowerCase()) <= -1) {
                                    positive_return = true;
                                    break;
                                }
                            }

                            var negative_return = false;
                            for (var j = 0; j < filterNegativeSplits.length; j++) {
                                if (filterNegativeSplits[j] !== '' && text.indexOf($.trim(filterNegativeSplits[j]).replace(/ +/g, ' ').toLowerCase()) > -1) {
                                    positive_return = true;
                                    break;
                                }
                            }

                            return (positive_return || negative_return);
                        }).hide();
                    } else {
                        $elements.show()
                    }

                    setTimeout(function () {
                        var $spanPositiveFilter = $('span#filter-counter-positive-exp');
                        var $spanNegativeFilter = $('span#filter-counter-negative-exp');

                        var total = $('.exp-img-cont').length;
                        var vis = $('.exp-img-cont:visible').length;
                        var diff = total - vis;

                        if (filterPositiveSplits.length > 0) {
                            $spanPositiveFilter.text(vis + " / " + $elements.length);
                            $spanPositiveFilter.show();
                            projectEditor.folderExplorer.last_positive_filter = textPositiveFilter;
                        } else {
                            $spanPositiveFilter.text('');
                            $spanPositiveFilter.hide();
                            projectEditor.folderExplorer.last_positive_filter = null;
                        }

                        if (filterNegativeSplits.length > 0) {
                            $spanNegativeFilter.text(diff + " / " + $elements.length);
                            $spanNegativeFilter.show();
                            projectEditor.folderExplorer.last_negative_filter = textNegativeFilter;
                        } else {
                            $spanNegativeFilter.text('');
                            $spanNegativeFilter.hide();
                            projectEditor.folderExplorer.last_negative_filter = null;
                        }
                    }, 150);
                },
                filterExplorerDebounce: debounce(function (input, e) {
                    var code = e.keyCode || e.which;
                    // tab=9, shift=16, ctrl=17 , arrows =37..40
                    if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                    if (code == '27') $(input).val('');
                    projectEditor.folderExplorer.filterExplorer();
                }, 250),


                showInExplorer: function (nodeId, callback) {
                    console.log("show in explorer, nodeId: " + nodeId);
                    $.ajax({
                        url: "/api/protected/contextMenuForEditor",
                        data: {action: 'EXPLORER_FOLDER', nodeId: nodeId, projectUuid: $('#project-editor-devenvironment-data').attr('data-project-uuid')},
                        type: 'POST',
                        timeout: 60000,
                        cache: false,
                        beforeSend: function () {
                            showBusysign();
                        },
                        success: function (data) {
                            // populate the template and show the modal window
                            var template = $('#apkreverse-folder-explorer-template').html();
                            var dataTemplate = {
                                data: data
                            };
                            var html = Mustache.to_html(template, dataTemplate);
                            $('#folder-explorer-current-path').text(data.current_path);
                            $('#folder-explorer-nb-element').text(data.elements.length);
                            $('#folder-explorer-container').html(html);
                            if (data.parentId) {
                                var $folderup = $('#folder-explorer-up');
                                $folderup.attr('data-up-folder', data.parentId);
                                $folderup.show();
                            } else {
                                $('#folder-explorer-up').hide();
                            }

                            // clean filters
                            $('#filter-positive-folder-exp-input').val('');
                            var $posSpanFilter = $('span#filter-counter-positive-exp');
                            $posSpanFilter.text('');
                            $posSpanFilter.hide();

                            $('#filter-negative-folder-exp-input').val('');
                            var $negSpanFilter = $('span#filter-counter-negative-exp');
                            $negSpanFilter.text('');
                            $negSpanFilter.hide();

                            $('#modalFolderExplorer').modal('show');
                            if (callback) {
                                callback.call();
                            }
                            projectEditor.folderExplorer.last_opened_node = nodeId;

                        },
                        error: function (xhr) {
                            var msg;
                            if (xhr.responseText === "undefined" || !xhr.responseText) {
                                msg = xhr.statusText;
                            } else {
                                msg = xhr.statusText + ": " + xhr.responseText;
                            }
                            notify(msg, "error", 7000);
                        },
                        complete: function () {
                            hideBusysign();
                        }
                    });
                },
                openToolbarExplorer: function () {
                    if (projectEditor.folderExplorer.last_opened_node) {
                        var myCallback = function () {

                            if (projectEditor.folderExplorer.last_positive_filter) {
                                $('#filter-positive-folder-exp-input').val(projectEditor.folderExplorer.last_positive_filter);
                            }
                            if (projectEditor.folderExplorer.last_negative_filter) {
                                $('#filter-negative-folder-exp-input').val(projectEditor.folderExplorer.last_negative_filter);
                            }
                            projectEditor.folderExplorer.filterExplorer();


                        };
                        projectEditor.folderExplorer.showInExplorer(projectEditor.folderExplorer.last_opened_node, myCallback);
                    } else {
                        projectEditor.folderExplorer.showInExplorer('0');
                    }
                }

            }
        };

        // apk ready wrapper
        var apk_ready = {
            apkReadyCloseNotification: function (e, clickedElement) {
                e.preventDefault();
                var li = $(clickedElement).closest('li');
                li.fadeOut({queue: false, duration: 500}).animate({ height: 0 }, 500, function () {
                    li.remove();
                });
            },
            apkReadyDownloadFile: function (e, clickedElement) {
                e.preventDefault();
                var tmpApkPath = $(clickedElement).closest('li').attr('data-apk-filepath');
                console.log('Downloading APK file :' + tmpApkPath);
                try {
                    // remove previous iframe
                    var previousDownloadFrame = document.getElementById('iframe-tmp-apk-download');
                    if (previousDownloadFrame !== null) {
                        previousDownloadFrame.parentNode.removeChild(previousDownloadFrame);
                    }
                    // add new iframe
                    var downloadFrame = document.createElement("iframe");
                    downloadFrame.id = 'iframe-tmp-apk-download';
                    downloadFrame.setAttribute('class', "downloadFrameScreen");
                    var url = window.location.protocol + "//" + window.location.hostname + ":" + window.location.port
                        + "/api/protected/downloadTmpApkFile?tmpApkPath=" + encodeURIComponent(tmpApkPath);
                    downloadFrame.setAttribute('src', url);
                    document.body.appendChild(downloadFrame);
                } catch (err) {
                    notify(err.toString(), "error", 7000);
                }
                /*
                 var downloadFrame = document.createElement("iframe");
                 downloadFrame.setAttribute('src', apkUrl);
                 downloadFrame.setAttribute('class', "downloadFrameScreen");
                 document.getElementById("apktools-previwNtest-data").appendChild(downloadFrame);
                 */
                //document.body.appendChild(downloadFrame);
            },
            apkReadyQrCode: function (e, clickedElement) {
                e.preventDefault();
                var apkUrl = $(clickedElement).closest('li').attr('data-apk-ready-url');


                var apkQrCodeDiv = $("#apk-ready-qrcode");
                apkQrCodeDiv.empty();
                apkQrCodeDiv.html("<canvas id=\"apk-ready-qrcode-canvas\"></canvas>");
                new QRious({
                    element: document.getElementById('apk-ready-qrcode-canvas'),
                    value: apkUrl,
                    size: 300
                });
                // open modal
                $('#modalApkReadyQrCode').modal('show');
            },
            apkReadyAdbInstall: function (e, clickedElement) {
                e.preventDefault();
                if ($(clickedElement).hasClass('anchor-disabled'))
                    return;
                var apkFilePath = $(clickedElement).closest('li').attr('data-apk-filepath');
                var packageName = $(clickedElement).attr('data-apk-pname');
                // ajax install apk
                $.ajax({
                    // when using localhost for adb and AnotherHostName for other request ==> CORS problem, not sending credentials
                    url: "/api/protected/androidDebugBridgeHandler",
                    data: {action: 'WIRE_INSTALL', apkFilePath: apkFilePath, packageName: packageName},
                    type: 'POST',
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    success: function (data) {
                        // Disable the anchor tag
                        $(clickedElement).attr('data-process-id', data.processId);
                        $(clickedElement).addClass('anchor-disabled').removeAttr("href");
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                    }
                });
            },
            apkReadyCreateNewProjectFromBenchmark: function (clickedElement) {
                var apkFilePath = $(clickedElement).closest('li').attr('data-apk-filepath');
                console.log('Creating project from apk : ' + apkFilePath);
                // open modal create new project from benchmark
            }
        };


        ApkToolApi.getInstance = function () {
            if (!instance) {
                instance = _createInstance();
                virtualTerminal.initJQueryTerminal();
            }
            return instance;
        };
        ApkToolApi.init = function () {
            // init apk tool lib
            var ApkTool = ApkToolApi.getInstance();
            // observe : this observer resets side menu log counter depending on apkreverse_content visibility
            var target = document.querySelector('#apkreverse_content');
            var observerLogEvent = new MutationObserver(function (mutations) {
                mutations.forEach(function (mutation) {
                    if (mutation.attributeName === 'style'
                        && window.getComputedStyle(target).getPropertyValue('display') !== 'none') {
                        virtualTerminal.hideAndResetSideMenuEventCounter();
                        if ($('div#tab-apkreverse-project-editor').length != 0) {
                            // Resize fileEditor otherwise it won't stretch to fit its parent
                            try {
                                w2ui['layout_file_editor'].resize();
                            } catch (err) {
                                // do nothing
                            }
                        }

                        // Resize Debugger layout otherwise it won't stretch to fit its parent
                        try {
                            w2ui['layout_debugger'].resize();
                        } catch (err) {
                            // do nothing
                        }

                    }
                });
            });
            // Attach the mutation observer to 'apkreverse_content', and only when attribute values change
            observerLogEvent.observe(target, {attributes: true});
            // init UI
            main_UI_loader.initUi();
            projectsListWrapper.reloadUiProjectsList();
            keystoreListWrapper.reloadUiKeystoresList();
        };
        ApkToolApi.reloadUiProjectsList = function () {
            projectsListWrapper.reloadUiProjectsList();
        };
        ApkToolApi.reloadUiKeystoresList = function () {
            keystoreListWrapper.reloadUiKeystoresList();
        };
        ApkToolApi.openCreateProjectModal = function (e) {
            main_UI_loader.openCreateProjectModal(e);
        };
        ApkToolApi.benchmarkApk = function () {
            benchmark.benchmarkApk();
        };
        ApkToolApi.cancelBenchmarkApk = function () {
            benchmark.cancelBenchmarkApk();
        };
        ApkToolApi.minimizeVirtualTerminal = function (e) {
            virtualTerminal.minimizeVirtualTerminal(e);
        };
        ApkToolApi.maximizeVirtualTerminal = function (e) {
            virtualTerminal.maximizeVirtualTerminal(e);
        };
        ApkToolApi.hideAndResetSideMenuEventCounter = function () {
            virtualTerminal.hideAndResetSideMenuEventCounter();
        };
        ApkToolApi.webSocketMessagingProtocol = function (msg) {
            webSocketWrapper.webSocketMessagingProtocol(msg);
        };
        ApkToolApi.apkReadyCloseNotification = function (e, clickedElement) {
            apk_ready.apkReadyCloseNotification(e, clickedElement);
        };
        ApkToolApi.apkReadyDownloadFile = function (e, clickedElement) {
            apk_ready.apkReadyDownloadFile(e, clickedElement);
        };
        ApkToolApi.apkReadyQrCode = function (e, clickedElement) {
            apk_ready.apkReadyQrCode(e, clickedElement);
        };
        ApkToolApi.apkReadyAdbInstall = function (e, clickedElement) {
            apk_ready.apkReadyAdbInstall(e, clickedElement);
        };
        ApkToolApi.apkReadyCreateNewProjectFromBenchmark = function (clickedElement) {
            apk_ready.apkReadyCreateNewProjectFromBenchmark(clickedElement);
        };
        ApkToolApi.openNewProjectSubModal = function () {
            main_UI_loader.openNewProjectSubModal();
        };
        ApkToolApi.createNewProject = function () {
            projectsListWrapper.createNewProject();
        };
        ApkToolApi.getUserProjectsTotalSize = function () {
            main_UI_loader.getUserProjectsTotalSize();
        };
        ApkToolApi.openProject = function (e, clickedElement) {
            projectsListWrapper.openProject(e, clickedElement);
        };
        ApkToolApi.cloneProject = function (e, clickedElement) {
            projectsListWrapper.cloneProject(e, clickedElement);
        };
        ApkToolApi.detailsProject = function (e, clickedElement) {
            projectsListWrapper.detailsProject(e, clickedElement);
        };
        ApkToolApi.deleteProject = function (e, clickedElement) {
            projectsListWrapper.deleteProject(e, clickedElement);
        };
        ApkToolApi.backToProjectsList = function () {
            projectsListWrapper.backToProjectsList();
        };
        ApkToolApi.renameFileOrFolder = function (nodeId) {
            projectEditor.contextMenuWrapper.renameItem_execute_rename(nodeId);
        };
        ApkToolApi.deleteFileOrFolder = function (nodeId) {
            projectEditor.contextMenuWrapper.deleteItem_execute_delete(nodeId);
        };
        ApkToolApi.createNewFolder = function (nodeId) {
            projectEditor.contextMenuWrapper.createFolderItem_execute_create(nodeId);
        };
        ApkToolApi.editorTabsContextMenuClicked = function (tabId, action) {
            projectEditor.tabsManager.editorTabsContextMenuClicked(tabId, action);
        };
        ApkToolApi.textSearchPerform = function () {
            projectEditor.textSearchPerform();
        };
        ApkToolApi.textSearchAndReplacePerform = function () {
            projectEditor.textSearchAndReplacePerform();
        };
        ApkToolApi.toolsPackageNameChangerPerform = function () {
            projectEditor.toolsPackageNameChangerPerform();
        };
        ApkToolApi.toolsAppInfoUpdaterPerform = function () {
            projectEditor.toolsAppInfoUpdaterPerform();
        };
        ApkToolApi.toolsPackageRenamerPerform = function () {
            projectEditor.toolsPackageRenamerPerform();
        };
        ApkToolApi.toolsManifestEntriesRenamerPerform = function () {
            projectEditor.toolsManifestEntriesRenamerPerform();
        };
        ApkToolApi.toolsAppNameModifierPerform = function () {
            projectEditor.toolsAppNameModifierPerform();
        };
        ApkToolApi.toolsBuildApkForReleasePerform = function () {
            projectEditor.toolsBuildApkForReleasePerform();
        };
        ApkToolApi.cancelUserProcess = function (btn) {
            projectEditor.cancelUserProcess(btn);
        };
        ApkToolApi.openCreateKeystoretModal = function (e) {
            keystoreListWrapper.openCreateKeystoretModal(e);
        };
        ApkToolApi.openImportKeystoreModal = function (e) {
            keystoreListWrapper.openImportKeystoreModal(e);
        };
        ApkToolApi.newKeystoreNextStep = function () {
            keystoreListWrapper.newKeystoreNextStep();
        };
        ApkToolApi.createNewKeystore = function () {
            keystoreListWrapper.createNewKeystore();
        };
        ApkToolApi.feApkTabInfo = function (e, apkFileName, apkType) {
            projectEditor.bottomTabsManager.apkFile.feApkTabInfo(e, apkFileName, apkType);
        };
        ApkToolApi.feApkTabInstall = function (e, apkFileName, apkType) {
            projectEditor.bottomTabsManager.apkFile.feApkTabInstall(e, apkFileName, apkType);
        };
        ApkToolApi.feApkTabDownload = function (e, apkFileName, apkType) {
            projectEditor.bottomTabsManager.apkFile.feApkTabDownload(e, apkFileName, apkType);
        };
        ApkToolApi.feApkTabRemove = function (e, apkFileName) {
            projectEditor.bottomTabsManager.apkFile.feApkTabRemove(e, apkFileName);
        };
        ApkToolApi.feApkTabNewProject = function (e, apkFileName) {
            projectEditor.bottomTabsManager.apkFile.feApkTabNewProject(e, apkFileName);
        };
        ApkToolApi.keystoreActions = function (e, element, action) {
            keystoreListWrapper.keystoreActions(e, element, action);
        };
        ApkToolApi.importKeystore = function () {
            keystoreListWrapper.importKeystore();
        };
        ApkToolApi.keystoreSubmitUpdate = function () {
            keystoreListWrapper.keystoreSubmitUpdate();
        };
        ApkToolApi.adbInstallApk = function () {
            adbInstall.adbInstallkApk();
        };
        ApkToolApi.cancelAdbInstallApk = function () {
            adbInstall.cancelAdbInstallApk();
        };
        ApkToolApi.onDebuggerProcessSelected = function () {
            appDebugger.onDebuggerProcessSelected();
        };
        return ApkToolApi;
    };

    exports.MApkTool = MApkTool;
    window.ApkToolModule = new MApkTool;
}(this, jQuery);

$(function () {
    ApkToolModule.init();
});