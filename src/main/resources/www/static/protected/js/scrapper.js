!function (exports, $, undefined) {
    var MScrapper = function () {
        var ScrapperApi = {}; // public api

        var instance;

        function MScrapper() {
        }

        function _createInstance() {
            return new MScrapper();
        }

        function getInstance() {
            if (!instance) {
                instance = _createInstance();
            }
            return instance;
        }


        function __formatPlayStoreImage(path, newSize) {
            if (path === null || path === '')
                return null;

            // regex to detect google resize parameters which is something like path=h310-rw or path=w310-rw
            var regex1 = /=[\h\\w\\s]\w+(-{1})rw/;
            // no -rw at the end
            var regex2 = /=[\h\\w\\s]\w+/;
            if (newSize) {
                if (path.match(regex1)) {
                    path = path.replace(regex1, "=h" + newSize);
                } else {
                    path = path.replace(regex2, "=h" + newSize);
                }
            } else {
                if (path.match(regex1)) {
                    path = path.replace(regex1, "");
                } else {
                    path = path.replace(regex2, "");
                }
            }

            if (!path.startsWith("http")) {
                path = "https:" + path;
            }
            return path;
        }

        var webSocketWrapper = {
            webSocketMessagingProtocol: function (msg) { // wraps the ws communication protocol between the scrapper and server
                var jsonData = JSON.parse(msg.data);
                if (jsonData.dataType) {
                    if (jsonData.dataType === 'scrapper-event') {
                        if (jsonData.dataLogType) {
                            if (jsonData.dataLogType === 'COUNTRY_PROGRESS') {
                                // set progress
                                $('#progress-country-scrapper> span').text(jsonData.dataMsg + "%");
                            } else if (jsonData.dataLogType === 'SINGLE_APP_PROGRESS') {
                                // set progress
                                $('#progress-appsearch-scrapper> span').text(jsonData.dataMsg + "%");
                            }
                        }
                    } else if (jsonData.dataType === 'scrapper-proc-state') {
                        // update UI depending process type and state
                        var processType = jsonData.dataProcessType;
                        var state = jsonData.dataState;
                        var receivedProcessId = jsonData.dataProcessId;
                        if (processType === 'COUNTRY_PROGRESS') {
                            if (state === 'STARTED') {
                                $('#progress-country-scrapper> span').text('0%');
                                $('#progress-country-scrapper').show();
                                // block country scrapper form until process end
                                $('#search-appcountries-form').block({
                                    message: "It may take from 4 to 7 minutes, Please wait...",
                                    centerY: false,
                                    centerX: false,
                                    css: {
                                        border: 'none',
                                        padding: '5px',
                                        width: '35%',
                                        backgroundColor: '#000',
                                        '-webkit-border-radius': '10px',
                                        '-moz-border-radius': '10px',
                                        opacity: .5,
                                        color: '#fff',
                                        position: 'absolute',
                                        margin: 'auto'
                                    }
                                });
                                appCountries.appCountriesFormWasSubmitted = true;
                            } else if (state === 'COMPLETED') {
                                $('#progress-country-scrapper').hide();
                                $('#progress-country-scrapper> span').text('0%');
                                $('#cancel-appcountries-scrapper').attr('data-process-id', '');
                                // unblock form
                                $('#search-appcountries-form').unblock();
                                toastr.success('Country scrapper has ended with success!', 'App Countries');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Country scrapper has ended with success.");
                                }, 700);
                                // ajax get result
                                if (jsonData.dataResultUuid) {
                                    appCountries.getNonAvailableCountriesResult(jsonData.dataResultUuid);
                                }
                                appCountries.appCountriesFormWasSubmitted = false;
                            } else if (state === 'ERROR') {
                                $('#progress-country-scrapper').hide();
                                $('#progress-country-scrapper> span').text('0%');
                                $('#cancel-appcountries-scrapper').attr('data-process-id', '');
                                // unblock form
                                $('#search-appcountries-form').unblock();
                                toastr.error('Country scrapper has ended with errors!', 'App Countries');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Country scrapper has ended with errors.");
                                }, 700);
                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                                appCountries.appCountriesFormWasSubmitted = false;
                            }
                        } else if (processType === 'SINGLE_APP_PROGRESS') {
                            if (state === 'STARTED') {
                                $('#progress-appsearch-scrapper> span').text('0%');
                                $('#progress-appsearch-scrapper').show();
                                // block country scrapper form until process end
                                $('#search-singleapp-form').block({
                                    message: "It may take less than 2 minutes, Please wait...",
                                    centerY: false,
                                    centerX: false,
                                    css: {
                                        border: 'none',
                                        padding: '5px',
                                        width: '35%',
                                        backgroundColor: '#000',
                                        '-webkit-border-radius': '10px',
                                        '-moz-border-radius': '10px',
                                        opacity: .5,
                                        color: '#fff',
                                        position: 'absolute',
                                        margin: 'auto'
                                    }
                                });
                                singleAppScrapper.singleAppFormWasSubmitted = true;
                            } else if (state === 'COMPLETED') {
                                $('#progress-appsearch-scrapper').hide();
                                $('#progress-appsearch-scrapper> span').text('0%');
                                $('#cancel-appsearch-scrapper').attr('data-process-id', '');
                                // unblock form
                                $('#search-singleapp-form').unblock();
                                toastr.success('Application details has ended with success!', 'Application details');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Application details has ended with success.");
                                }, 700);
                                // ajax get result
                                if (receivedProcessId) {
                                    singleAppScrapper.getSingleAppResult(receivedProcessId);
                                }
                                singleAppScrapper.singleAppFormWasSubmitted = false;
                            } else if (state === 'ERROR') {
                                $('#progress-appsearch-scrapper').hide();
                                $('#progress-appsearch-scrapper> span').text('0%');
                                $('#cancel-appsearch-scrapper').attr('data-process-id', '');
                                // unblock form
                                $('#search-singleapp-form').unblock();
                                toastr.error('Application details has ended with errors!', 'Application details ');
                                $.playSound('/static/public/sounds/bell.mp3');
                                setTimeout(function () {
                                    textToSpeech("Application details has ended with errors.");
                                }, 700);
                                if (jsonData.dataMessage) {
                                    notify(jsonData.dataMessage, "error", 7000);
                                }
                                singleAppScrapper.singleAppFormWasSubmitted = false;
                            }
                        }
                    }
                }
            }
        };

        var general = {
            openProxySettings: function () {
                $('#modal-kw-search-confirm-no-proxy').modal('hide');
                $('#modalProxy').modal('show');
            },
            installOnDevice: function (e, appId, appName) {
                e.preventDefault();

                var url = location.protocol + '//' + location.host + '/api/protected/remoteinstruction';

                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }
                var installOnDevice_beforeSendCallback = function () {
                    var title = (appName) ? appName : appId;
                    $.blockUI({
                        message: "Opening &laquo;" + title + "&raquo; Please wait...",
                        css: {
                            border: 'none',
                            padding: '15px',
                            backgroundColor: '#000',
                            '-webkit-border-radius': '10px',
                            '-moz-border-radius': '10px',
                            opacity: .5,
                            color: '#fff'
                        } });
                };
                var installOnDevice_successCallback = function (data) {
                    notify(data, 'success', 5000);
                };
                var installOnDevice_completeCallback = function () {
                    $.unblockUI();
                };
                remoteInstruction({action: "INSTALL_APP", app_id: appId}, 35 * 1000, installOnDevice_beforeSendCallback, installOnDevice_successCallback, null, installOnDevice_completeCallback);
            },
            openGPAppPage: function (event, language_code) {
                event.preventDefault();
                var app_id = $('#app-id-hidden').val();
                if (app_id !== null && app_id !== '')
                    window.open("https://play.google.com/store/apps/details?id=" + app_id + "&hl=" + language_code);
            },
            downloadApk: function (e, packageName) {
                e.preventDefault();
                if (packageName === undefined || packageName === "") {
                    notify("Bad package name", "warning", 5000);
                    return;
                }

                try {
                    // remove previous iframe
                    var previousDownloadFrame = document.getElementById('iframe-apk-download');
                    if (previousDownloadFrame != null) {
                        previousDownloadFrame.parentNode.removeChild(previousDownloadFrame);
                    }
                    // add new iframe
                    var downloadFrame = document.createElement("iframe");
                    downloadFrame.id = 'iframe-apk-download';
                    downloadFrame.setAttribute('class', "downloadFrameScreen");
                    var url = "https://apkpure.com/x/" + packageName + "/download?from=details";
                    downloadFrame.setAttribute('src', 'javascript:window.location.replace("' + url + '")');
                    /*downloadFrame.onload = function () {
                     return function () {
                     // check if it is a 404
                     var div404 = $("#iframe-apk-download").contents().find("div.text-404");
                     if(div404){
                     alert("oops this APK is not found on apkpure!");
                     }

                     }
                     }();*/
                    document.body.appendChild(downloadFrame);
                    notify("Apk download will start in few seconds, Please wait...", "success", 5000);
                } catch (err) {
                    notify(err.toString(), "error", 7000);
                }
            },
            openApkDownloadPage: function (e, packageName) {
                e.preventDefault();
                if (packageName === undefined || packageName === "") {
                    notify("Bad package name", "warning", 5000);
                    return;
                }
                var url = "https://apkpure.com/x/" + packageName;
                window.open(url, '_blank');
            },
            getAppASO: function (e, packageName, typeAso) {
                e.preventDefault();
                if (packageName === undefined || packageName === "") {
                    notify("Bad package name", "warning", 5000);
                    return;
                }
                if (typeAso === undefined || typeAso === "") {
                    notify("Bad aso type:" + typeAso, "warning", 5000);
                    return;
                }
                switch (typeAso) {
                    case 'appBrain':
                    {
                        //window.open(appBrainUrl,'_blank','toolbar=0,location=0,menubar=0');
                        var appBrainUrl = "https://www.appbrain.com/app/" + packageName;
                        window.open(appBrainUrl, '_blank');
                        return;
                    }
                    case 'searchman':
                    {
                        //window.open(searchmanUrl,'_blank','toolbar=0,location=0,menubar=0');
                        var searchmanUrl = "https://searchman.com/android/app/us/" + packageName + "/en/x/x/";
                        window.open(searchmanUrl, '_blank');
                        return;
                    }
                    case 'appAnnie':
                    {
                        //window.open(appAnnieUrl,'_blank','toolbar=0,location=0,menubar=0');
                        var appAnnieUrl = "https://www.appannie.com/en/apps/google-play/app/" + packageName + "/#";
                        window.open(appAnnieUrl, '_blank');
                        return;
                    }
                    default :
                    {
                        notify('unknown ASO provider: ' + type);
                    }
                }
            }
        };

        var keywordSearch = {
            keywordFormWasSubmitted: false,
            filterAppSearch: function (textFilter) {
                var appList = $('ul#list-result-kwsearch');
                var $rows = appList.find('li.list-group-item');
                var val = $.trim(textFilter).replace(/ +/g, ' ').toLowerCase();

                $rows.show().filter(function () {
                    var text = $(this).find("span.appname-sp").text().replace(/\s+/g, ' ').toLowerCase()
                        + $(this).find("span.developer-account-sp").text().replace(/\s+/g, ' ').toLowerCase();
                    //var text = $(this).text().replace(/\s+/g, ' ').toLowerCase();
                    return !~text.indexOf(val);
                }).hide();

                if (val) {
                    var indic = $('ul#list-result-kwsearch li:visible').length + " / " + appList.children().length;
                    $('span.filter-counter').text(indic);
                } else {
                    $('span.filter-counter').text('');
                }
            },
            proceedKeywordSearch: function (mustUseProxy) {
                if (keywordSearch.keywordFormWasSubmitted) {
                    notify('Please wait until server response!', 'information', 3000);
                    return;
                }

                var url = location.protocol + '//' + location.host + '/api/protected/keywordapi';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                var scrap_language_code, scrap_language_name, scrap_country_code, scrap_country_name, proxy_host, proxy_port, user_agent_scrap, scrap_keyword;


                var $scrap_language_select = document.getElementById("scrap_language_select");
                scrap_language_code = $scrap_language_select.options[$scrap_language_select.selectedIndex].value;
                scrap_language_name = $scrap_language_select.options[$scrap_language_select.selectedIndex].text;


                var $scrap_country_select = document.getElementById("scrap_country_select");
                scrap_country_code = $scrap_country_select.options[$scrap_country_select.selectedIndex].value;
                scrap_country_name = $scrap_country_select.options[$scrap_country_select.selectedIndex].text;

                proxy_host = RevEnge.getProxyData().active_proxy.host;
                proxy_port = RevEnge.getProxyData().active_proxy.port;
                scrap_keyword = $('#scrap_keyword').val();

                // form validation

                if (scrap_language_code === '') {
                    notify('Please select a language', 'error', 2000);
                    return;
                }

                if (scrap_country_code === '') {
                    notify('Please select a country', 'error', 2000);
                    return;
                }

                if (scrap_keyword === '') {
                    notify('Please enter a keyword', 'error', 2000);
                    return;
                }

                if (mustUseProxy) {
                    if (proxy_host === null || proxy_host === '') {
                        // Show confirmation (continue without a proxy)
                        $('#modal-kw-search-confirm-no-proxy').modal('show');
                        return;
                    } else {
                        if (proxy_port === null || proxy_port === '') {
                            // Show confirmation (continue without a proxy)
                            $('#modal-kw-search-confirm-no-proxy').modal('show');
                            return;
                        }
                    }
                }

                // Make ajax call to server
                var paramObj = {action: "FIRST_PAGE"};
                $.each($('#search-keyword-form').serializeArray(), function (_, kv) {
                    paramObj[kv.name] = kv.value;
                });

                if (proxy_host !== null && proxy_host !== '' && proxy_port !== null && proxy_port !== '') {
                    paramObj.proxy_host = proxy_host;
                    paramObj.proxy_port = proxy_port;
                }

                console.log('sending Kw search request : ' + JSON.stringify(paramObj));
                $.ajax({
                    type: 'GET',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 1 minutes

                    beforeSend: function () {
                        // setting a timeout
                        //showBusysign();
                        $('#search-keyword-form').block({
                            message: "It may take few seconds, Please wait...",
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
                        keywordSearch.keywordFormWasSubmitted = true;
                        //$(placeholder).addClass('loading');
                    },
                    success: function (data) {
                        // *****************
                        if (data.apps === null || data.apps === undefined) {
                            $('#kw-search-duration').text('0 result');
                            notify('Received an empty result!!!', 'error', 5000);
                            //hideBusysign();
                            $('#search-keyword-form').unblock();
                            keywordSearch.keywordFormWasSubmitted = false;
                            return;
                        } else {
                            var msg;
                            if (data.apps.length > 1) {
                                msg = 'found ' + data.apps.length + ' results in ' + data.duration + ' seconds';
                            } else {
                                msg = 'found ' + data.apps.length + ' result in ' + data.duration + ' seconds';
                            }
                            $('#kw-search-duration').text(msg).show();
                            notify(msg, 'success', 5000);
                        }

                        // reset filters text
                        $('.filter-app-list').val('');
                        $('span.filter-counter').text('');

                        // empty list
                        var kwResultList = $("ul#list-result-kwsearch");
                        kwResultList.empty();

                        //alert('server response success:' + JSON.stringify(data));
                        var template = $('#kw-search-result-li-template').html();
                        for (var i = 0; i < data.apps.length; i++) {
                            //console.log(data.apps[i]);
                            var icon = __formatPlayStoreImage(data.apps[i].icon, 70);
                            var count = i + 1;

                            var dataTmpl = {
                                counter: count,
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.apps[i].developer, " ", "+"),
                                icon: icon,
                                app: data.apps[i]
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            kwResultList.append(html);
                        }
                        var moreBtn = $('button#show-more-button');
                        if (data.pagination) {
                            moreBtn.attr('data-pagination', data.pagination);
                            moreBtn.show();
                        } else {
                            moreBtn.attr('data-pagination', '');
                            moreBtn.hide();
                        }
                        //hideBusysign();
                        keywordSearch.keywordFormWasSubmitted = false;
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        keywordSearch.keywordFormWasSubmitted = false;
                    },
                    complete: function () {
                        //hideBusysign();
                        $('#search-keyword-form').unblock();
                        keywordSearch.keywordFormWasSubmitted = false;
                    }
                });
            },

            // get app of the next page
            loadMoreResult: function (button) {
                var pagination = $(button).attr("data-pagination");
                if (!pagination)
                    return;
                var loader = $('div#bottom-loading-gif');
                // Make ajax call
                var url = location.protocol + '//' + location.host + '/api/protected/keywordapi';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }
                var proxy_host = RevEnge.getProxyData().active_proxy.host;
                var proxy_port = RevEnge.getProxyData().active_proxy.port;

                var paramObj = {action: "NEXT_PAGE", pagination: pagination, proxy_host: proxy_host, proxy_port: proxy_port};
                console.log('sending Kw search request : ' + JSON.stringify(paramObj));

                $.ajax({
                    type: 'GET',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 1 minute

                    beforeSend: function () {
                        $(button).hide();
                        loader.show();
                        // setting a timeout
                        //showBusysign();
                        //keywordSearch.keywordFormWasSubmitted = true;
                        //$(placeholder).addClass('loading');
                    },
                    success: function (data) {
                        // *****************
                        if (!data.apps) {
                            //$('#kw-search-duration').text('0 result');
                            notify('Received an empty result!!!', 'error', 5000);
                            //hideBusysign();
                            //keywordSearch.keywordFormWasSubmitted = false;
                            $(button).show();
                            loader.hide();
                            return;
                        } else {
                            var msg;
                            if (data.apps.length > 1) {
                                msg = 'found ' + data.apps.length + ' results in ' + data.duration + ' seconds';
                            } else {
                                msg = 'found ' + data.apps.length + ' result in ' + data.duration + ' seconds';
                            }
                            $('#kw-search-duration').text(msg).show();
                            notify(msg, 'success', 5000);
                        }

                        var kwResultList = $("ul#list-result-kwsearch");
                        var countLi = kwResultList.children().length;
                        //kwResultList.empty();
                        //alert('server response success:' + JSON.stringify(data));
                        var template = $('#kw-search-result-li-template').html();
                        for (var i = 0; i < data.apps.length; i++) {
                            //console.log(counter.counter_name);
                            var icon = __formatPlayStoreImage(data.apps[i].icon, 70);
                            var count = countLi + i + 1;

                            var dataTmpl = {
                                counter: count,
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.apps[i].developer, " ", "+"),
                                icon: icon,
                                app: data.apps[i]
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            kwResultList.append(html);
                        }
                        if (data.pagination) {
                            $('button#show-more-button').attr('data-pagination', data.pagination);
                            $(button).show();
                            loader.hide();
                        } else {
                            $('button#show-more-button').attr('data-pagination', '');
                            $(button).hide();
                            loader.hide();
                        }
                        var filterValue = $('#kw-filter-in').val();
                        if (filterValue) {
                            keywordSearch.filterAppSearch(filterValue);
                        }

                        //hideBusysign();
                        //keywordSearch.keywordFormWasSubmitted = false;
                    },
                    error: function (xhr) {
                        $(button).show();
                        loader.hide();
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        //keywordSearch.keywordFormWasSubmitted = false;
                    },
                    complete: function () {
                        //$(button).hide();
                        loader.hide();
                        //hideBusysign();
                        //keywordSearch.keywordFormWasSubmitted = false;
                    }
                });

            },

            submitKeywordScrapperForm: function (event) {
                event.preventDefault();
                //keywordSearch.proceedKeywordSearch(true);
                keywordSearch.proceedKeywordSearch(false);
            },

            confirmKwSearchWithoutProxy: function () {
                $('#modal-kw-search-confirm-no-proxy').modal('hide');
                keywordSearch.proceedKeywordSearch(false);
            },
            resetKeywordScrapperForm: function (event) {
                event.preventDefault();
                var $lang_sel = $('#scrap_language_select');
                var msLanguageSelect = $lang_sel.msDropDown().data("dd");
                if (msLanguageSelect) {
                    msLanguageSelect.set("selectedIndex", 24);
                } else {
                    $lang_sel.val('en-US');
                }

                var $country_sel = $('#scrap_country_select');
                var msCountrySelect = $country_sel.msDropDown().data("dd");
                if (msCountrySelect) {
                    msCountrySelect.set("selectedIndex", 230);
                } else {
                    $country_sel.val('US');
                }
                $("form#search-keyword-form input[name=appprice][value='1']").prop("checked", true);
                $('#scrap_keyword').val('');
                notify('keyword search form has been reset', 'success', 1000);
            },
            clearKeywordResults: function (event) {
                event.preventDefault();
                $('#kw-search-duration').text('').hide();
                var $buttonLoadMore = $('button#show-more-button');
                $buttonLoadMore.attr('data-pagination', '');
                $buttonLoadMore.hide();
                $('div#bottom-loading-gif').hide();
                // reset filters text
                $('.filter-app-list').val('');
                $('span.filter-counter').text('');
                // empty the list
                $("ul#list-result-kwsearch").empty();
            }
        };

        var singleAppScrapper = {
            singleAppFormWasSubmitted: false,
            proceedSingleAppSearch: function (mustUseProxy) {
                if($("ul#single-app-scrapper-results").children('li').length >= 50){
                    notify("You can't store more than 50 APP_DETAILS result, please remove few ones then continue!", "error", 5000);
                    return;
                }
                if (singleAppScrapper.singleAppFormWasSubmitted) {
                    notify('Please wait until server response!', 'information', 3000);
                    return;
                }

                var url = location.protocol + '//' + location.host + '/api/protected/multilangscrapperapi';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                var app_id, proxy_host, proxy_port;

                app_id = $('#appId-singleapp').val().trim();

                proxy_host = RevEnge.getProxyData().active_proxy.host;
                proxy_port = RevEnge.getProxyData().active_proxy.port;


                if (app_id === '') {
                    notify('Please enter a package name or the corresponding google play url', 'error', 2000);
                    return;
                }

                var patternUrl = /^https:\/\/play\.google\.com\/store\/apps\/details\?id=(([A-Za-z0-9]{1}[A-Za-z0-9\\d_]*\.)+[A-Za-z0-9][A-Za-z0-9\\d_]*)$/m;
                var patternPackageName = /^(([A-Za-z0-9]{1}[A-Za-z0-9\\d_]*\.)+[A-Za-z0-9][A-Za-z0-9\\d_]*)$/m;

                if (!app_id.match(patternUrl) && !app_id.match(patternPackageName)) {
                    notify('Incorrect package name or url : ' + app_id, 'error', 4000);
                    return;
                }

                if (mustUseProxy) {
                    if (proxy_host === null || proxy_host === '') {
                        // Show confirmation (continue without a proxy)
                        $('#modal-singleapp-search-confirm-no-proxy').modal('show');
                        return;
                    } else {
                        if (proxy_port === null || proxy_port === '') {
                            //notify('Proxy port is empty ==> you are not using a proxy!', 'warning', '3000');
                            // Show confirmation (continue without a proxy)
                            $('#modal-singleapp-search-confirm-no-proxy').modal('show');
                            return;
                        }
                    }
                }
                // Make ajax call to server
                var paramObj = {action: 'SINGLE_APP_SCRAPPER'};
                $.each($('#search-singleapp-form').serializeArray(), function (_, kv) {
                    paramObj[kv.name] = kv.value;
                });

                if (proxy_host !== null && proxy_host !== '' && proxy_port !== null && proxy_port !== '') {
                    paramObj.proxy_host = proxy_host;
                    paramObj.proxy_port = proxy_port;
                }
                console.log('sending single app scrapping request : ' + JSON.stringify(paramObj));
                $.ajax({
                    type: 'POST',
                    url: url,
                    data: paramObj,
                    async: true,
                    timeout: 20 * 1000,

                    success: function (data) {
                        // set process id => to be canceled on user demand
                        $('#cancel-appsearch-scrapper').attr('data-process-id', data);
                        // let WebSocket take care of the rest
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
            cancelSingleAppScrapping: function () {
                var $btn_cancel = $('#cancel-appsearch-scrapper');
                $btn_cancel.attr('disabled', true);
                $.ajax({
                    url: '/api/protected/multilangscrapperapi',
                    data: {action: 'CANCEL_PROCESS', processId: $btn_cancel.attr('data-process-id')},
                    type: 'POST',
                    dataType: 'json',
                    async: true,
                    timeout: 20 * 1000,
                    success: function (data) {
                        $('#progress-appsearch-scrapper').hide();
                        $btn_cancel.attr('data-process-id', '');
                        $('#progress-appsearch-scrapper> span').text('0%');
                        // unblock form
                        $('#search-singleapp-form').unblock();
                        singleAppScrapper.singleAppFormWasSubmitted = false;
                        notify('Process canceled successfully!', 'success', 5000);
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
                        $btn_cancel.attr('disabled', false);
                    }
                });
            },
            getSingleAppResult: function (processId) {
                //console.log('getting SingleApp results...');
                var url = '/api/protected/multilangscrapperapi';
                // Make ajax call to server
                var paramObj = {action: 'GET_SINGLE_APP_RESULT', processId: processId};

                $.ajax({
                    type: 'POST',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 20 * 1000,

                    success: function (data) {
                        var result = data.result;
                        if (result === null || result === "undefined") {
                            $('#search-duration').text('0 result');
                            notify('Received empty result!!!', 'error', 5000);
                        } else {
                            var minutes = Math.floor(data.duration / 60);
                            var seconds = parseInt(data.duration - (minutes * 60));
                            var searchDuration = "";
                            if (minutes && minutes !== 0) {
                                searchDuration = minutes + " minutes";
                            }
                            if (seconds) {
                                if (searchDuration === '') {
                                    searchDuration = seconds + " seconds";
                                } else {
                                    searchDuration += " and " + seconds + " seconds";
                                }
                            }
                            $('#search-duration').text('found results in ' + searchDuration).show();

                            var type = result.type;
                            var template_bean = result.content.details;

                            if (!template_bean) {
                                notify('Bad result data', 'error', 5000);
                                return;
                            }

                            var id = singleAppScrapper.mLocalStorage.saveAppResultToLocal(result);
                            if (id === null) {
                                id = generateUUID();
                            }

                            var template = $('#app-search-result-li-template').html();
                            var dataTemplate = {
                                template_bean: template_bean,
                                id: id,
                                app_id: result.app_id,
                                language_code: result.lang,
                                isFree: (template_bean.price === "0"),
                                type: type,
                                icon_src: __formatPlayStoreImage(template_bean.icon, 130),
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(template_bean.developer, " ", "+")
                            };
                            var html = Mustache.to_html(template, dataTemplate);
                            $("ul#single-app-scrapper-results").prepend(html);
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
            submitSingleAppScrapperForm: function (event) {
                event.preventDefault();
                //singleAppScrapper.proceedSingleAppSearch(true);
                singleAppScrapper.proceedSingleAppSearch(false);
            },
            getAppDetails: function (e, packageName) {
                e.preventDefault();
                if (singleAppScrapper.singleAppFormWasSubmitted) {
                    notify("Please wait for the current search to finish!", "warning", 5000);
                    return;
                }
                $('#appId-singleapp').val(packageName);
                // show tab
                $('ul#scrapper-navtab a[href="#scrapper-single-app-data"]').tab('show');
                // start scrapping
                singleAppScrapper.proceedSingleAppSearch(false);
            },
            confirmSingleAppSearchWithoutProxy: function () {
                $('#modal-singleapp-search-confirm-no-proxy').modal('hide');
                singleAppScrapper.proceedSingleAppSearch(false);
            },
            hideVideoFrameCaption: function () {
                $('#video-caption').hide();
            },
            openDetailsAppSearch: function (button) {
                var localStorageId = $(button).closest('li').attr('id');
                var appDetails = singleAppScrapper.mLocalStorage.getAppResultFromLocal(localStorageId);

                if (appDetails !== null) {
                    var type = appDetails.type;
                    //console.log(JSON.stringify(appDetails));

                    if (type === 'mono') {
                        var template_bean = appDetails.content;
                        var other_screenshots = [];
                        if (template_bean.screenshots.length > 1) {
                            for (var i = 1; i < template_bean.screenshots.length; i++) {
                                other_screenshots.push(__formatPlayStoreImage(template_bean.screenshots[i], 250))
                            }
                        }
                        // build modal body
                        var img_src = __formatPlayStoreImage(template_bean.icon, 250);

                        var screenshotsIndex = 2;
                        var templateBody = $('#app-full-details-template').html();
                        var dataTemplateBody = {
                            template_bean: template_bean,
                            id: localStorageId,
                            app_id: appDetails.app_id,
                            language: appDetails.lang,
                            isFree: (template_bean.price === "0"),
                            type: type,
                            is_mono_language: (type === 'mono'),
                            is_multi_language: (type === 'multi'),
                            screenshotsIndex: function () {
                                return screenshotsIndex++;
                            },
                            first_screenshot: __formatPlayStoreImage(template_bean.screenshots[0], 250),
                            other_screenshots: other_screenshots,
                            icon_src: img_src,
                            developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(template_bean.developer, " ", "+")
                        };
                        var html = Mustache.to_html(templateBody, dataTemplateBody);
                        $("div#modal-app-full-details-content").html(html);

                    } else if (type === 'multi') {
                        //template_bean = appDetails.content.details;
                        var template_bean_multi = appDetails.content.details;
                        var other_screenshots_multi = [];
                        if (template_bean_multi.screenshots) {
                            for (var j = 1; j < template_bean_multi.screenshots.length; j++) {
                                other_screenshots_multi.push(__formatPlayStoreImage(template_bean_multi.screenshots[j], 250))
                            }
                        }
                        // build modal body
                        var img_src_multi = __formatPlayStoreImage(template_bean_multi.icon, 250);
                        var screenshotsIndex_multi = 2;
                        // an Id to associate accordions
                        var newRandomId = generateUUID();
                        var tempo = 0;
                        var templateBody_multi = $('#app-full-details-template').html();

                        //In Mustache templating, an elegant way of expressing a comma separated list without the trailing comma
                        // french, english,spanish, => french, english,spanish
                        for (var k = 0; k < appDetails.content.noDuplicates.length; k++) {
                            var nd = appDetails.content.noDuplicates[k];
                            nd.shortDescriptionsAndTitlesPerLanguage[nd.shortDescriptionsAndTitlesPerLanguage.length - 1].last = true;
                        }

                        var dataTemplateBody_multi = {
                            template_bean: template_bean_multi,
                            id: localStorageId,
                            app_id: appDetails.app_id,
                            language: appDetails.lang,
                            isFree: (template_bean_multi.price === "0"),
                            type: type,
                            is_mono_language: (type === 'mono'),
                            is_multi_language: (type === 'multi'),
                            screenshotsIndex: function () {
                                return screenshotsIndex_multi++;
                            },
                            first_screenshot: (template_bean_multi.screenshots ? __formatPlayStoreImage(template_bean_multi.screenshots[0], 250):"") ,
                            other_screenshots: other_screenshots_multi,
                            icon_src: img_src_multi,
                            random_id: function () {
                                switch (tempo) {
                                    case 0:
                                        tempo++;
                                        break;
                                    case 1:
                                        tempo++;
                                        break;
                                    case 2:
                                        newRandomId = generateUUID();
                                        tempo = 1;
                                        break;
                                }
                                return newRandomId;
                            },
                            developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(template_bean_multi.developer, " ", "+"),
                            noDuplicates: appDetails.content.noDuplicates
                        };
                        var html_multi = Mustache.to_html(templateBody_multi, dataTemplateBody_multi);
                        $("div#modal-app-full-details-content").html(html_multi);

                        // Android apps Accordion click event handler
                        $('.accordion-section-title').click(function (e) {
                            // Grab current anchor value
                            var currentAttrValue = $(this).attr('href');

                            if ($(e.target).is('.active')) {
                                // Remove active class to section title
                                $(this).removeClass('active');
                                // Close the hidden content panel
                                $('.accordion ' + currentAttrValue).slideUp(300).removeClass('open');
                            } else {
                                // Add active class to section title
                                $(this).addClass('active');
                                // Open up the hidden content panel
                                $('.accordion ' + currentAttrValue).slideDown(300).addClass('open');
                            }
                            e.preventDefault();
                        });
                        // if many languages associated with one Long description, the shorten the accordion title
                        var accordionsAnchors = $('a.accordion-lang');
                        accordionsAnchors.each(function (idx, a) {
                            var nbLanguages = $(a).attr('nb-lang');
                            if (nbLanguages > 4) {
                                var text = $(a).text();
                                var splits = text.split(',');
                                var diff = nbLanguages - 4;
                                var postfix = (diff > 1) ? 'languages' : 'language';
                                text = splits[0] + ',' + splits[1] + ',' + splits[2] + ',' + splits[3] + ',...+' + diff + ' ' + postfix;
                                $(a).text(text.trim());
                            }
                        });

                    } else {
                        notify('Bad data type', 'error', 5000);
                        return;
                    }
                    // prevent carousel from auto slide
                    $('.carousel').carousel({
                        interval: false
                    });
                    $('#modal-app-full-details').modal('show');
                } else {
                    notify('This item was not found in the cache!', 'error', 5000);
                }

            },
            downloadAppGraphics: function (event) {
                event.preventDefault();
                notify("Graphic files download will start in few seconds, Please wait...", "success", 5000);

                var app_storage_id = $('#app-storage-key-hidden').val();
                var app_id = $('#app-id-hidden').val();
                var app = singleAppScrapper.mLocalStorage.getAppResultFromLocal(app_storage_id);
                if (app === null || app === undefined)
                    return;

                var zip = new JSZip();
                var count = 0;
                var zipFilename = "graphics(" + app_id + ").zip";


                var details;
                if (app.type === 'mono') {
                    details = app.content;
                } else if (app.type === 'multi') {
                    details = app.content.details;
                }

                var icon_url = __formatPlayStoreImage(details.icon);
                console.log('url icon ++++++' + icon_url);
                var screenshots_urls = [];
                if (details.screenshots.length > 0) {
                    for (var i = 0; i < details.screenshots.length; i++) {
                        var screenshotHD = __formatPlayStoreImage(details.screenshots[i], 1000);
                        screenshots_urls.push(screenshotHD);
                    }
                }


                JSZipUtils.getBinaryContent(icon_url, function (err, data) {
                    if (err) {
                        throw err; // or handle the error
                    }
                    zip.file("ic_launcher.png", data, {binary: true});
                });

                // TODO Loader
                var k = 0;
                screenshots_urls.forEach(function (url) {
                    var filename = "screen_shot_" + k + ".png";
                    // loading a file and add it in a zip file
                    JSZipUtils.getBinaryContent(url, function (err, data) {
                        if (err) {
                            throw err; // or handle the error
                        }
                        zip.file(filename, data, {binary: true});
                        count++;
                        if (count == screenshots_urls.length) {
                            zip.generateAsync({type: "blob"})
                                .then(function (content) {
                                    // see FileSaver.js
                                    saveAs(content, zipFilename);
                                });


                        }
                    });
                    k++;
                });
            },
            deleteAppSearch: function (e, packageName, localStorageId) {
                e.preventDefault();
                w2confirm('Do want to remove this item?')
                    .yes(function () {
                        console.log("deleting :" + packageName + ", storageId: " + localStorageId);
                        // delete from local storage
                        singleAppScrapper.mLocalStorage.deleteAppResultFromLocal(localStorageId);
                        // delete from html list with a smooth animation
                        var $li = $('ul#single-app-scrapper-results > li#' + localStorageId);
                        $li.fadeOut({queue: false, duration: 500}).animate({ height: 0 }, 500, function () {
                            $li.remove();
                        });
                    })
                    .no(function () {
                    });
            },
            getQuotaInfoAppDetails: function(e){
                if(e){
                    e.preventDefault();
                }
                // ajax get quota info and show in abstractModalWindow
                $.ajax({
                    url: '/api/protected/multilangscrapperapi',
                    data: {action: 'GET_QUOTA_INFO'},
                    type: 'POST',
                    dataType: 'text',
                    async: true,
                    timeout: 15 * 1000,
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
            mLocalStorage: {
                saveAppResultToLocal: function (data) {
                    if (typeof(Storage) === "undefined")
                        return null;
                    var indexDbStr = localStorage.getItem('single_app_search_index');
                    var indexDb;
                    if (indexDbStr !== null) {
                        indexDb = JSON.parse(indexDbStr);
                    } else {
                        indexDb = [];
                    }
                    var randomId = generateUUID();
                    data.storage_id = randomId;
                    indexDb.push(randomId);
                    localStorage.setItem('single_app_search_index', JSON.stringify(indexDb));
                    localStorage.setItem(randomId, JSON.stringify(data));
                    return randomId;
                },
                getAppResultFromLocal: function (id) {
                    if (typeof(Storage) === "undefined")
                        return null;
                    var object = localStorage.getItem(id);
                    if (object === null) {
                        return null;
                    } else {
                        return JSON.parse(object);
                    }

                },
                getAllAppResultsFromLocal: function () {
                    if (typeof(Storage) === "undefined")
                        return null;
                    var indexDbStr = localStorage.getItem('single_app_search_index');
                    if (indexDbStr === null) {
                        return null;
                    } else {
                        var result = [];
                        var indexDb = JSON.parse(indexDbStr);
                        for (var i = 0; i < indexDb.length; i++) {
                            result.push(JSON.parse(localStorage.getItem(indexDb[i])));
                        }
                        return result;
                    }
                },
                deleteAppResultFromLocal: function (id) {
                    if (typeof(Storage) === "undefined")
                        return;
                    var indexDbStr = localStorage.getItem('single_app_search_index');
                    if (indexDbStr !== null) {
                        var indexDb = JSON.parse(indexDbStr);
                        var indexOfRemove = indexDb.indexOf(id)
                        if (indexOfRemove > -1) {
                            indexDb.splice(indexOfRemove, 1);
                        }
                        // remove element
                        localStorage.removeItem(id);
                        // update indexes
                        localStorage.setItem('single_app_search_index', JSON.stringify(indexDb));
                    }
                },
                deleteAllAppResultsFromLocal: function () {
                    if (typeof(Storage) === "undefined")
                        return;
                    var indexDbStr = localStorage.getItem('single_app_search_index');
                    if (indexDbStr !== null) {
                        var indexDb = JSON.parse(indexDbStr);
                        for (var i = 0; i < indexDb.length; i++) {
                            localStorage.removeItem(indexDb[i]);
                        }
                        localStorage.setItem('single_app_search_index', JSON.stringify([]));
                    }
                },

                restoreAppResultsUi: function () {
                    if (typeof(Storage) === "undefined")
                        return;

                    var listApps = singleAppScrapper.mLocalStorage.getAllAppResultsFromLocal();
                    if (listApps !== null && listApps.length > 0) {
                        for (var i = 0; i < listApps.length; i++) {
                            var app = listApps[i];
                            var type = app.type;
                            var template_bean;

                            if (type === 'mono') {
                                template_bean = app.content;
                            } else if (type === 'multi') {
                                template_bean = app.content.details;
                            }

                            if (template_bean !== null && template_bean !== undefined) {
                                var template = $('#app-search-result-li-template').html();
                                var dataTemplate = {
                                    template_bean: template_bean,
                                    id: app.storage_id,
                                    app_id: app.app_id,
                                    language_code: app.lang,
                                    isFree: (template_bean.price === "0"),
                                    type: type,
                                    language: app.lang,
                                    icon_src: __formatPlayStoreImage(template_bean.icon, 130),
                                    developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(template_bean.developer, " ", "+")
                                };
                                var html = Mustache.to_html(template, dataTemplate);
                                $("ul#single-app-scrapper-results").prepend(html);
                            }

                        }
                    }
                }
            }
        };

        var topAppsScrapping = {
            topAppsFormWasSubmitted: false,
            filterTopAppsSearch: function (textFilter) {
                var appList = $('ul#list-result-topapps-search');
                var $rows = appList.find('li.list-group-item');
                var val = $.trim(textFilter).replace(/ +/g, ' ').toLowerCase();

                $rows.show().filter(function () {
                    var text = $(this).find("span.applist-appname-sp").text().replace(/\s+/g, ' ').toLowerCase()
                        + $(this).find("span.applist-developer-sp").text().replace(/\s+/g, ' ').toLowerCase();
                    return !~text.indexOf(val);
                }).hide();

                if (val) {
                    var indic = $('ul#list-result-topapps-search li:visible').length + " / " + appList.children().length;
                    $('span.topapps-filter-counter').text(indic);
                } else {
                    $('span.topapps-filter-counter').text('');
                }
            },
            proceedTopAppsSearch: function (mustUseProxy) {
                if (topAppsScrapping.topAppsFormWasSubmitted) {
                    notify('Please wait until server response!', 'information', 3000);
                    return;
                }

                var url = location.protocol + '//' + location.host + '/api/protected/topapps';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                var language_code, language_name, country_code, country_name, collection_param, collection_label, category_param, category_label, proxy_host, proxy_port;

                var $language_select = document.getElementById("topapps_language_select");
                language_code = $language_select.options[$language_select.selectedIndex].value;
                language_name = $language_select.options[$language_select.selectedIndex].text;

                var $country_select = document.getElementById("topapps_country_select");
                country_code = $country_select.options[$country_select.selectedIndex].value;
                country_name = $country_select.options[$country_select.selectedIndex].text;

                var $collection_select = document.getElementById("topapps_collection_select");
                collection_param = $collection_select.options[$collection_select.selectedIndex].value;
                collection_label = $collection_select.options[$collection_select.selectedIndex].text;

                var $category_select = document.getElementById("topapps_category_select");
                category_param = $category_select.options[$category_select.selectedIndex].value;
                category_label = $category_select.options[$category_select.selectedIndex].text;

                proxy_host = RevEnge.getProxyData().active_proxy.host;
                proxy_port = RevEnge.getProxyData().active_proxy.port;


                // form validation

                if (language_code === '') {
                    notify('Please select a language', 'error', 2000);
                    return;
                }

                if (country_code === '') {
                    notify('Please select a country', 'error', 2000);
                    return;
                }

                if (mustUseProxy) {
                    if (proxy_host === null || proxy_host === '') {
                        // Show confirmation (continue without a proxy)
                        $('#modal-topapps-search-confirm-no-proxy').modal('show');
                        return;
                    } else {
                        if (proxy_port === null || proxy_port === '') {
                            // Show confirmation (continue without a proxy)
                            $('#modal-topapps-search-confirm-no-proxy').modal('show');
                            return;
                        }
                    }
                }

                // Make ajax call to server
                var paramObj = {action: "FIRST_PAGE"};
                $.each($('#search-topapps-form').serializeArray(), function (_, kv) {
                    paramObj[kv.name] = kv.value;
                });
                if (proxy_host !== null && proxy_host !== '' && proxy_port !== null && proxy_port !== '') {
                    paramObj.proxy_host = proxy_host;
                    paramObj.proxy_port = proxy_port;
                }
                console.log('sending TopApps search request : ' + JSON.stringify(paramObj));
                $.ajax({
                    type: 'GET',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 1 minutes

                    beforeSend: function () {
                        $('#search-topapps-form').block({
                            message: "It may take few seconds, Please wait...",
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
                        topAppsScrapping.topAppsFormWasSubmitted = true;
                    },
                    success: function (data) {
                        if (data.apps === null || data.apps === undefined) {
                            $('#topapps-search-duration').text('0 result');
                            notify('Received an empty result!!!', 'error', 5000);
                            $('#search-topapps-form').unblock();
                            topAppsScrapping.topAppsFormWasSubmitted = false;
                            return;
                        } else {
                            var msg;
                            var msg_prefix, category_desc = '';
                            if (category_param !== "ALL") {
                                category_desc = " and category '" + category_label + "'";
                            }
                            msg_prefix = collection_label + " apps in " + country_name + " for language '" + language_name + "'" + category_desc;
                            if (data.apps.length > 1) {
                                msg = '(found ' + data.apps.length + ' results in ' + data.duration + ' seconds)';
                            } else {
                                msg = '(found ' + data.apps.length + ' result in ' + data.duration + ' seconds)';
                            }
                            $('#topapps-search-duration').text(msg_prefix + ' ' + msg).show();
                            notify(msg, 'success', 5000);
                        }

                        // reset filters text
                        $('.filter-topapps-list').val('');
                        $('span.topapps-filter-counter').text('');

                        // empty list
                        var topAppsResultList = $("ul#list-result-topapps-search");
                        topAppsResultList.empty();

                        var template = $('#topapps-search-result-li-template').html();

                        for (var i = 0; i < data.apps.length; i++) {
                            var icon = __formatPlayStoreImage(data.apps[i].icon, 80);
                            var count = i + 1;

                            var dataTmpl = {
                                counter: count,
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.apps[i].developer, " ", "+"),
                                icon: icon,
                                app: data.apps[i]
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            topAppsResultList.append(html);
                        }

                        var moreBtn = $('button#show-more-topapps-button');
                        if (data.pagination) {
                            moreBtn.attr('data-next-page', data.nextPageNumber);
                            moreBtn.attr('data-language', data.language);
                            moreBtn.attr('data-country', data.country);
                            moreBtn.attr('data-collection', data.collection);
                            moreBtn.attr('data-category', data.category);
                            moreBtn.show();
                        } else {
                            moreBtn.attr('data-next-page', '');
                            moreBtn.attr('data-language', '');
                            moreBtn.attr('data-country', '');
                            moreBtn.attr('data-collection', '');
                            moreBtn.attr('data-category', '');
                            moreBtn.hide();
                        }
                        topAppsScrapping.topAppsFormWasSubmitted = false;
                    },
                    error: function (xhr) {
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        topAppsScrapping.topAppsFormWasSubmitted = false;
                    },
                    complete: function () {
                        $('#search-topapps-form').unblock();
                        topAppsScrapping.topAppsFormWasSubmitted = false;
                    }
                });
            },
            resetTopAppsForm: function (e) {
                e.preventDefault();
                var $lang_sel = $('#topapps_language_select');
                var msLanguageSelect = $lang_sel.msDropDown().data("dd");
                if (msLanguageSelect) {
                    msLanguageSelect.set("selectedIndex", 24);
                } else {
                    $lang_sel.val('en-US');
                }

                var $country_sel = $('#topapps_country_select');
                var msCountrySelect = $country_sel.msDropDown().data("dd");
                if (msCountrySelect) {
                    msCountrySelect.set("selectedIndex", 230);
                } else {
                    $country_sel.val('US');
                }
                $('#topapps_collection_select').val('topselling_free');
                $('#topapps_category_select').val('ALL');
                notify('TOP APPS form has been reset', 'success', 1000);
            },
            clearTopAppsResults: function (e) {
                e.preventDefault();
                $('#topapps-search-duration').text('').hide();
                var $buttonLoadMore = $('button#show-more-topapps-button');
                $buttonLoadMore.attr('data-next-page', '');
                $buttonLoadMore.attr('data-language', '');
                $buttonLoadMore.attr('data-country', '');
                $buttonLoadMore.attr('data-collection', '');
                $buttonLoadMore.attr('data-category', '');
                $buttonLoadMore.hide();
                $('div#bottom-topapps-loading-gif').hide();
                // reset filters text
                $('.filter-topapps-list').val('');
                $('span.topapps-filter-counter').text('');
                // empty the list
                $("ul#list-result-topapps-search").empty();
            },
            submitTopAppsForm: function (e) {
                e.preventDefault();
                //topAppsScrapping.proceedTopAppsSearch(true);
                topAppsScrapping.proceedTopAppsSearch(false);
            },
            confirmTopAppsSearchWithoutProxy: function () {
                $('#modal-topapps-search-confirm-no-proxy').modal('hide');
                topAppsScrapping.proceedTopAppsSearch(false);
            },
            loadMoreTopAppsResult: function (button) {
                var nextPage = $(button).attr("data-next-page");
                var language = $(button).attr("data-language");
                var country = $(button).attr("data-country");
                var collection = $(button).attr("data-collection");
                var category = $(button).attr("data-category");

                if (!nextPage || !language || !country || !collection || !category) {
                    notify("incorrect parameters for LOAD_MORE TopApps!")
                    return;
                }

                var loader = $('div#bottom-topapps-loading-gif');
                // Make ajax call
                var url = location.protocol + '//' + location.host + '/api/protected/topapps';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                var proxy_host = RevEnge.getProxyData().active_proxy.host;
                var proxy_port = RevEnge.getProxyData().active_proxy.port;

                var paramObj = {action: "LOAD_MORE", nextPage: nextPage, language: language, country: country, collection: collection, category: category, proxy_host: proxy_host, proxy_port: proxy_port};
                console.log('sending TopApps load_more request : ' + JSON.stringify(paramObj));

                // ajax call
                $.ajax({
                    type: 'GET',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 1 minute

                    beforeSend: function () {
                        $(button).hide();
                        loader.show();
                    },
                    success: function (data) {
                        // *****************
                        if (!data.apps) {
                            $('#topapps-search-duration').text('0 result');
                            notify('Received an empty result!!!', 'error', 5000);
                            $(button).show();
                            loader.hide();
                            return;
                        } else {
                            // fetch language, country, collection and category by their id
                            var language_label = language, country_label = country, collection_label = collection, category_label = category;

                            $.each(iso_countries, function (index) {
                                if (iso_countries[index].code === country) {
                                    country_label = iso_countries[index].name;
                                    return false;
                                }
                            });

                            $.each(google_description_languages, function (index) {
                                if (google_description_languages[index].code === language) {
                                    language_label = google_description_languages[index].language;
                                    return false;
                                }
                            });

                            $.each(google_app_collections, function (index) {
                                if (google_app_collections[index].param === collection) {
                                    collection_label = google_app_collections[index].label;
                                    return false;
                                }
                            });

                            var msg, msg_prefix, category_desc = '';
                            if (category !== "ALL") {
                                $.each(google_app_categories, function (index) {
                                    if (google_app_categories[index].param === category) {
                                        category_label = google_app_categories[index].label;
                                        return false;
                                    }
                                });
                                category_desc = " and category '" + category_label + "'";
                            }
                            msg_prefix = collection_label + " apps in " + country_label + " for language '" + language_label + "'" + category_desc;
                            if (data.apps.length > 1) {
                                msg = '(found ' + data.apps.length + ' results in ' + data.duration + ' seconds)';
                            } else {
                                msg = '(found ' + data.apps.length + ' result in ' + data.duration + ' seconds)';
                            }
                            $('#topapps-search-duration').text(msg_prefix + ' ' + msg).show();
                            notify(msg, 'success', 5000);
                        }

                        var topAppsResultList = $("ul#list-result-topapps-search");
                        var countLi = topAppsResultList.children().length;

                        var template = $('#topapps-search-result-li-template').html();
                        for (var i = 0; i < data.apps.length; i++) {
                            var icon = __formatPlayStoreImage(data.apps[i].icon, 80);
                            var count = countLi + i + 1;

                            var dataTmpl = {
                                counter: count,
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.apps[i].developer, " ", "+"),
                                icon: icon,
                                app: data.apps[i]
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            topAppsResultList.append(html);
                        }


                        if (data.pagination) {
                            $(button).attr('data-next-page', data.nextPageNumber);
                            $(button).attr('data-language', data.language);
                            $(button).attr('data-country', data.country);
                            $(button).attr('data-collection', data.collection);
                            $(button).attr('data-category', data.category);
                            $(button).show();
                        } else {
                            $(button).attr('data-next-page', '');
                            $(button).attr('data-language', '');
                            $(button).attr('data-country', '');
                            $(button).attr('data-collection', '');
                            $(button).attr('data-category', '');
                            $(button).hide();
                        }
                        loader.hide();

                        var filterValue = $('#topapps-filter-in').val();
                        if (filterValue) {
                            topAppsScrapping.filterTopAppsSearch(filterValue);
                        }
                    },
                    error: function (xhr) {
                        $(button).show();
                        loader.hide();
                        var msg;
                        if (xhr.responseText === "undefined" || !xhr.responseText) {
                            msg = xhr.statusText;
                        } else {
                            msg = xhr.statusText + ": " + xhr.responseText;
                        }
                        notify(msg, "error", 7000);
                        //keywordSearch.keywordFormWasSubmitted = false;
                    },
                    complete: function () {
                        //$(button).hide();
                        loader.hide();
                        //hideBusysign();
                        //keywordSearch.keywordFormWasSubmitted = false;
                    }
                });

            }
        };

        var appCountries = {
            appCountriesFormWasSubmitted: false,
            submitAppCountriesForm: function (event) {
                event.preventDefault();
                //appCountries.proceedAppCountriesSearch(true);
                appCountries.proceedAppCountriesSearch(false);
            },
            confirmAppCountriesWithoutProxy: function () {
                $('#modal-appcountries-search-confirm-no-proxy').modal('hide');
                appCountries.proceedAppCountriesSearch(false);
            },
            proceedAppCountriesSearch: function (mustUseProxy) {
                if (appCountries.appCountriesFormWasSubmitted) {
                    notify('Please wait until server response!', 'information', 3000);
                    return;
                }

                var url = location.protocol + '//' + location.host + '/api/protected/appcountries';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                var app_id, proxy_host, proxy_port;

                app_id = $('#appid_countries').val().trim();
                if (app_id === '') {
                    notify('Please enter a package name or the corresponding google play url', 'error', 2000);
                    return;
                }

                var patternUrl = /^https:\/\/play\.google\.com\/store\/apps\/details\?id=(([A-Za-z0-9]{1}[A-Za-z0-9\\d_]*\.)+[A-Za-z0-9][A-Za-z0-9\\d_]*)$/m;
                var patternPackageName = /^(([A-Za-z0-9]{1}[A-Za-z0-9\\d_]*\.)+[A-Za-z0-9][A-Za-z0-9\\d_]*)$/m;

                if (!app_id.match(patternUrl) && !app_id.match(patternPackageName)) {
                    notify('Incorrect package name or url : ' + app_id, 'error', 2000);
                    return;
                }

                proxy_host = RevEnge.getProxyData().active_proxy.host;
                proxy_port = RevEnge.getProxyData().active_proxy.port;

                if (mustUseProxy) {
                    if (proxy_host === null || proxy_host === '') {
                        // Show confirmation (continue without a proxy)
                        $('#modal-appcountries-search-confirm-no-proxy').modal('show');
                        return;
                    } else {
                        if (proxy_port === null || proxy_port === '') {
                            // Show confirmation (continue without a proxy)
                            $('#modal-appcountries-search-confirm-no-proxy').modal('show');
                            return;
                        }
                    }
                }

                // Make ajax call to server
                var paramObj = {action: 'SCRAP_COUNTRIES'};
                $.each($('#search-appcountries-form').serializeArray(), function (_, kv) {
                    paramObj[kv.name] = kv.value;
                });

                if (proxy_host !== null && proxy_host !== '' && proxy_port !== null && proxy_port !== '') {
                    paramObj.proxy_host = proxy_host;
                    paramObj.proxy_port = proxy_port;
                }
                console.log('sending AppCountries search request : ' + JSON.stringify(paramObj));

                $.ajax({
                    type: 'POST',
                    url: url,
                    data: paramObj,
                    async: true,
                    timeout: 20 * 1000,

                    success: function (data) {
                        // set process id => to be canceled on user demand
                        $('#cancel-appcountries-scrapper').attr('data-process-id', data);
                        // let WebSocket take care of the rest
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
            getNonAvailableCountriesResult: function (resultUuid) {
                console.log('getting AppCountries results...');
                var url = location.protocol + '//' + location.host + '/api/protected/appcountries';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                // Make ajax call to server
                var paramObj = {action: 'GET_COUNTRIES_RESULT', resultUuid: resultUuid};

                $.ajax({
                    type: 'POST',
                    url: url,
                    data: paramObj,
                    dataType: 'json',
                    async: true,
                    timeout: 20 * 1000,

                    success: function (data) {
                        //console.log("non available countries are : " + JSON.stringify(data));
                        if (data) {
                            // update ui => add it to list
                            var nonAvailableCountries = [];
                            if (data.nonAvailableCountries) {
                                $.each(data.nonAvailableCountries, function (i) {
                                    var flag_src = "static/public/images/flags/16/" + data.nonAvailableCountries[i].toLowerCase() + ".png";
                                    var country_name = "NaN";
                                    $.each(iso_countries, function (j) {
                                        if (iso_countries[j].code === data.nonAvailableCountries[i]) {
                                            country_name = iso_countries[j].name;
                                            return false;
                                        }
                                    });
                                    nonAvailableCountries.push({flag_src: flag_src, country_name: country_name});
                                });
                            }

                            var template = $('#appcountries-result-li-template').html();
                            var dataTmpl = {
                                app: data,
                                appIconSrc: __formatPlayStoreImage(data.icon, 70),
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.developer, " ", "+"),
                                nonAvailableCountries: nonAvailableCountries,
                                countryCount: function () {
                                    var count = nonAvailableCountries.length;
                                    if (count === 1) {
                                        return " ONE country";
                                    } else {
                                        return count + " countries";
                                    }
                                }
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            $('#list-appcountries').prepend(html);
                            var $appcountriesDuration = $('#appcountries-search-duration');

                            var minutes = Math.floor(data.duration / 60);
                            var seconds = parseInt(data.duration - (minutes * 60));

                            $appcountriesDuration.text('found results in ' + minutes + ' minutes and ' + seconds + ' seconds');
                            $appcountriesDuration.show();
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
            initAppCountriesTab: function () {
                console.log("loading app countries...");
                var url = location.protocol + '//' + location.host + '/api/protected/appcountries';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                $.ajax({
                    type: 'POST',
                    url: url,
                    data: {action: 'GET_ALL'},
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 1 minutes

                    success: function (data) {
                        console.log('initializing app countries...');

                        // add element to favorite apps tab
                        var template = $('#appcountries-result-li-template').html();
                        var $appCountriesList = $('#list-appcountries');

                        $.each(data, function (index) {
                            var nonAvailableCountries = [];
                            if (data[index].nonAvailableCountries) {
                                $.each(data[index].nonAvailableCountries, function (i) {
                                    var flag_src = "static/public/images/flags/16/" + data[index].nonAvailableCountries[i].toLowerCase() + ".png";
                                    var country_name = "NaN";
                                    $.each(iso_countries, function (j) {
                                        if (iso_countries[j].code === data[index].nonAvailableCountries[i]) {
                                            country_name = iso_countries[j].name;
                                            return false;
                                        }
                                    });
                                    nonAvailableCountries.push({flag_src: flag_src, country_name: country_name});
                                });
                            }

                            var template = $('#appcountries-result-li-template').html();
                            var dataTmpl = {
                                app: data[index],
                                appIconSrc: __formatPlayStoreImage(data[index].icon, 70),
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data[index].developer, " ", "+"),
                                nonAvailableCountries: nonAvailableCountries,
                                countryCount: function () {
                                    var count = nonAvailableCountries.length;
                                    if (count === 1) {
                                        return " ONE country";
                                    } else {
                                        return count + " countries";
                                    }
                                }
                            };
                            var html = Mustache.to_html(template, dataTmpl);
                            $appCountriesList.append(html);
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
            },
            removeAppCountries: function (e, uuid) {
                e.preventDefault();
                w2confirm('Do want to remove this item?')
                    .yes(function () {
                        var url = location.protocol + '//' + location.host + '/api/protected/appcountries';
                        if (url.startsWith("file")) {
                            notify('Cannot reach the server!', 'error', 5000);
                            return;
                        }

                        $.ajax({
                            type: 'POST',
                            url: url,
                            data: {action: 'REMOVE_RESULT', uuid: uuid},
                            dataType: 'json',
                            async: true,
                            timeout: 20 * 1000,
                            beforeSend: function () {
                                showBusysign();
                            },
                            success: function (data) {
                                var $li = $('li.appcountries-content-li[data-uuid="' + uuid + '"]');

                                $li.fadeOut({queue: false, duration: 500}).animate({ height: 0 }, 500, function () {
                                    $li.remove();
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
                    })
                    .no(function () {
                    });
            },
            checkAppCountries: function (e, appId) {
                e.preventDefault();
                if (appCountries.appCountriesFormWasSubmitted) {
                    notify("Please wait for the current AppCountries to finish!", "warning", 5000);
                    return;
                }
                $('#appid_countries').val(appId);
                // show tab
                $('ul#scrapper-navtab a[href="#scrapper-country-detect-data"]').tab('show');
                // start scrapping
                appCountries.proceedAppCountriesSearch(false);
            },
            cancelAppCountriesScrapping: function () { // cancel current running benchmark
                var $btn_cancel = $('#cancel-appcountries-scrapper');
                $btn_cancel.attr('disabled', true);
                $.ajax({
                    url: '/api/protected/appcountries',
                    data: {action: 'CANCEL_PROCESS', processId: $btn_cancel.attr('data-process-id')},
                    type: 'POST',
                    dataType: 'json',
                    async: true,
                    timeout: 20 * 1000,
                    success: function (data) {
                        $('#progress-country-scrapper').hide();
                        $btn_cancel.attr('data-process-id', '');
                        $('#progress-country-scrapper> span').text('0%');
                        // unblock form
                        $('#search-appcountries-form').unblock();
                        appCountries.appCountriesFormWasSubmitted = false;
                        notify('Process canceled successfully!', 'success', 5000);
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
                        $btn_cancel.attr('disabled', false);
                    }
                });
            },
            getQuotaInfoAppCounty: function (e) {
                if (e) {
                    e.preventDefault();
                }
                // ajax get quota info and show in abstractModalWindow
                $.ajax({
                    url: '/api/protected/appcountries',
                    data: {action: 'GET_QUOTA_INFO'},
                    type: 'POST',
                    dataType: 'text',
                    async: true,
                    timeout: 15 * 1000,
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
            }
        };

        var favoriteApps = {
            filterFavoriteApps: function (textFilter) {
                //var appList = $('ul#list-result-topapps-search');
                //var $rows = appList.find('li.list-group-item');
                var availableDatesLists = $('ul.favoriteapps-per-day-list');
                $.each(availableDatesLists, function () {
                    $(this).closest('li').show();
                });

                var $rows = $('li.fav-app-content-li');
                var val = $.trim(textFilter).replace(/ +/g, ' ').toLowerCase();

                $rows.show().filter(function () {
                    var text = $(this).find("span.applist-appname-sp").text().replace(/\s+/g, ' ').toLowerCase()
                        + $(this).find("span.applist-developer-sp").text().replace(/\s+/g, ' ').toLowerCase()
                        + $(this).attr("title").replace(/\s+/g, ' ').toLowerCase();
                    return !~text.indexOf(val);
                }).hide();


                $.each(availableDatesLists, function () {
                    var $liContainer = $(this).closest('li');
                    if ($(this).find('li.fav-app-content-li:visible').length == 0) {
                        $liContainer.hide();
                    } else {
                        $liContainer.show();
                    }
                });

                if (val) {
                    var indic = $('li.fav-app-content-li:visible').length + " / " + $rows.length;
                    $('span.favoriteapps-filter-counter').text(indic);
                } else {
                    $('span.favoriteapps-filter-counter').text('');
                }
            },
            initFavoriteAppsTab: function () {
                console.log("loading favorite apps...");
                var url = location.protocol + '//' + location.host + '/api/protected/favoriteapps';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }

                $.ajax({
                    type: 'POST',
                    url: url,
                    data: {action: 'GET_ALL'},
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 2 minutes

                    beforeSend: function () {
                        // setting a timeout
                        showBusysign();
                    },
                    success: function (data) {
                        console.log('initializing Favorite apps...');

                        // add element to favorite apps tab
                        var template = $('#favoriteapps-result-li-template').html();
                        var favoriteAppsList = $('#list-result-favoriteapps');

                        var pattern = /(\d{4})(\d{2})(\d{2})/;
                        // key contains dates, values contains list<FavoriteApp> for that specific date
                        $.each(data, function (key, value) {
                            favoriteAppsList.prepend('<li data-date="' + key + '"/>');
                            var $li_date = $('ul#list-result-favoriteapps > li[data-date="' + key + '"]');

                            var match = pattern.exec(key);
                            var strDate = monthNames[match[2] - 1] + " " + match[3] + ", " + match[1];

                            $li_date.html("<div class='fav-app-date-div'>" + strDate + "</div><ul class='favoriteapps-per-day-list' data-date='" + key + "'></ul>");
                            var favoriteAppsPerDayList = $('ul.favoriteapps-per-day-list[data-date="' + key + '"]');
                            $.each(value, function (index) {
                                var app = value[index];
                                //console.log(value[index]);

                                var dataTmpl = {
                                    appIconSrc: __formatPlayStoreImage(app.icon, 70),
                                    developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(app.developer, " ", "+"),
                                    app: app
                                };
                                var html = Mustache.to_html(template, dataTmpl);
                                favoriteAppsPerDayList.append(html);
                            });
                        });
                        // set favorite apps tab badge

                        // show hide filter, zero favorite app div
                        var nbFavApps = $("li.fav-app-content-li").length;
                        if (nbFavApps > 1) {
                            $('#favoriteapps-search-filter').show();
                        } else {
                            $('#favoriteapps-search-filter').hide();
                        }
                        if (nbFavApps > 0) {
                            $('#no-favoriteapp-yet-div').hide();
                            $('#favapps-counter-badge').text(nbFavApps);
                        } else {
                            $('#no-favoriteapp-yet-div').show();
                            $('#favapps-counter-badge').text('');
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
            addAppToFavorite: function (e, packageName) {
                e.preventDefault();
                //TODO reset filter

                console.log("adding  '" + packageName + "' to favourite apps");

                var url = location.protocol + '//' + location.host + '/api/protected/favoriteapps';
                if (url.startsWith("file")) {
                    notify('Cannot reach the server!', 'error', 5000);
                    return;
                }
                var proxy_host = RevEnge.getProxyData().active_proxy.host;
                var proxy_port = RevEnge.getProxyData().active_proxy.port;


                $.ajax({
                    type: 'POST',
                    url: url,
                    data: {action: 'ADD', appId: packageName, proxy_host: proxy_host, proxy_port: proxy_port},
                    dataType: 'json',
                    async: true,
                    timeout: 60 * 1000, // sets timeout to 2 minutes

                    beforeSend: function () {
                        // setting a timeout
                        showBusysign();
                    },
                    success: function (data) {
                        if (data) {
                            data.free = data.isFree;
                            data.name = data.title;
                            // add element to favorite apps tab
                            var template = $('#favoriteapps-result-li-template').html();
                            var dataTmpl = {
                                appIconSrc: __formatPlayStoreImage(data.icon, 70),
                                developer_link: "https://play.google.com/store/apps/developer?id=" + replaceAll(data.developer, " ", "+"),
                                app: data
                            };
                            var html = Mustache.to_html(template, dataTmpl);

                            var favoriteAppsPerDayList = $('ul.favoriteapps-per-day-list[data-date="' + data.dataCreatedFormatted + '"]');

                            if (favoriteAppsPerDayList.length !== 0) { // if list exist, just add element to it
                                favoriteAppsPerDayList.prepend(html);
                            } else { // if not exist create it and add element to it
                                var pattern = /(\d{4})(\d{2})(\d{2})/;
                                var match = pattern.exec(data.dataCreatedFormatted);
                                var strDate = monthNames[match[2] - 1] + " " + match[3] + ", " + match[1];

                                var favoriteAppsList = $('ul#list-result-favoriteapps');
                                favoriteAppsList.prepend('<li data-date="' + data.dataCreatedFormatted + '"><div class="fav-app-date-div">' + strDate + '</div><ul class="favoriteapps-per-day-list" data-date="' + data.dataCreatedFormatted + '"></ul></li>');
                                var destinationList = $('ul.favoriteapps-per-day-list[data-date="' + data.dataCreatedFormatted + '"]');
                                destinationList.prepend(html);
                            }

                            // show hide filter, zero favorite app div
                            var nbFavApps = $("li.fav-app-content-li").length;
                            if (nbFavApps > 1) {
                                $('#favoriteapps-search-filter').show();
                            } else {
                                $('#favoriteapps-search-filter').hide();
                            }
                            if (nbFavApps > 0) {
                                $('#no-favoriteapp-yet-div').hide();
                                $('#favapps-counter-badge').text(nbFavApps);
                            } else {
                                $('#no-favoriteapp-yet-div').show();
                                $('#favapps-counter-badge').text('');
                            }


                            // update favorite apps tab badge
                            // notify success
                            notify(packageName + " added to favorite apps with success", "success", 5000);
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
            removeAppFromFavorite: function (e, packageName) {
                e.preventDefault();
                console.log("removing  '" + packageName + "' from favourite apps");
                w2confirm('Do want to remove "' + packageName + '" from Favorite Apps?')
                    .yes(function () {
                        var url = location.protocol + '//' + location.host + '/api/protected/favoriteapps';
                        if (url.startsWith("file")) {
                            notify('Cannot reach the server!', 'error', 5000);
                            return;
                        }

                        $.ajax({
                            type: 'POST',
                            url: url,
                            data: {action: 'REMOVE', appId: packageName},
                            dataType: 'json',
                            async: true,
                            timeout: 20 * 1000, // sets timeout to 2 minutes

                            beforeSend: function () {
                                // setting a timeout
                                showBusysign();
                            },
                            success: function (data) {
                                var $li = $('li.fav-app-content-li[data-app-id="' + packageName + '"]');

                                $li.fadeOut({queue: false, duration: 500}).animate({ width: 0 }, 500, function () {
                                    var $containerList = $li.closest('ul');
                                    $li.remove();
                                    if ($containerList) {
                                        var date = $containerList.attr('data-date');
                                        var $lis = $containerList.find('li');
                                        if ($lis.length === 0) {
                                            $('ul#list-result-favoriteapps > li[data-date="' + date + '"]').remove();
                                        }
                                    }

                                    // show hide filter, zero favorite app div
                                    var nbFavApps = $("li.fav-app-content-li").length;
                                    if (nbFavApps > 1) {
                                        $('#favoriteapps-search-filter').show();
                                    } else {
                                        $('#favoriteapps-search-filter').hide();
                                    }
                                    if (nbFavApps > 0) {
                                        $('#no-favoriteapp-yet-div').hide();
                                        $('#favapps-counter-badge').text(nbFavApps);
                                    } else {
                                        $('#no-favoriteapp-yet-div').show();
                                        $('#favapps-counter-badge').text('');
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
                    })
                    .no(function () {
                    });
            }
        };

        var ui = {
            initUi: function () {
                $('#modal-app-full-details').on('show.bs.modal', function () {
                    $('#modal-app-full-details .modal-body').css('overflow-y', 'auto');
                    $('#modal-app-full-details .modal-body').css('max-height', $(window).height() * 0.7);
                }).on('hide.bs.modal', function () {
                    $("div#modal-app-full-details-content").empty();
                });
                ui.initKeyWordSearch();
                ui.initSingleAppDetails();
                ui.initTopAppsSearch();
                ui.initFavoriteApps();
                ui.initAppCountries();
            },
            initKeyWordSearch: function () {
                // filter keyword search result
                var filterAppSearchDebounce = debounce(function (input, e) {
                    var code = e.keyCode || e.which;
                    // tab=9, shift=16, ctrl=17 , arrows =37..40
                    if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                    if (code == '27') $(input).val(null);
                    // same word in all filters inputs
                    $('.filter-app-list').val($(input).val());
                    keywordSearch.filterAppSearch($(input).val());
                }, 250);

                $('.filter-app-list').keyup(function (e) {
                    filterAppSearchDebounce(this, e);
                });

                // show/hide backup filter when scrolling keyword search
                $(window).scroll(debounce(function () {

                    if (!$('#scrapper_content').is(':visible') || !$('#scrapper-kw-search-data').hasClass('active')) {
                        return;
                    }

                    var kwFilterDiv = $('#kw-search-filter');
                    var kwFilterScrollDiv = $('#kw-search-filter-scroll');
                    var kwFilterInput = $('#kw-filter-in');
                    var kwFilterScrollInput = $('#kw-filter-in-scroll');

                    if (kwFilterDiv.isvisible(true)) {
                        if (!kwFilterScrollInput.is(":focus")) {
                            kwFilterScrollDiv.hide();
                        }
                    } else {
                        kwFilterScrollDiv.show();
                        if (kwFilterInput.is(":focus")) {
                            kwFilterScrollInput.focus();
                        }
                    }
                }, 250));

                // Observe the list of keyword search so we can update the counter
                var target = document.getElementById('list-result-kwsearch');
                var observer = new MutationObserver(function (mutations) {
                    mutations.forEach(function (mutation) {
                        var nbItems = $("ul#list-result-kwsearch").children().length;
                        //$('#nb-description').text(nbDesc);
                        if (nbItems > 1) {
                            $('#kw-search-filter').show();
                        } else {
                            $('#kw-search-filter').hide();
                        }
                    });
                });
                var config = { childList: true};
                observer.observe(target, config);

                var $language_select = $('#scrap_language_select');
                $language_select.append("<option hidden disabled value>-- select a language --</option>");
                var i = 0;
                $.each(google_description_languages, function () {
                    //$language_select.append($("<option />").val(google_description_languages[i].code).text(google_description_languages[i].language));
                    var code = google_description_languages[i].code;
                    var language = google_description_languages[i].language;
                    var flag = google_description_languages[i].flag.replace(".png", "");
                    $language_select.append($("<option data-image='static/public/plugins/ms-dropdown/images/icons/blank.gif' data-imagecss='flag " + flag + "' data-title='" + language + "' />").val(code).text(language));
                    i++;
                });
                $language_select.val('en-US');
                $language_select.msDropdown({visibleRows: 10});


                var $country_select = $("#scrap_country_select");
                $country_select.append("<option hidden disabled value>-- select a country --</option>");
                var j = 0;
                $.each(iso_countries, function () {
                    //country_select.append($("<option />").val(iso_countries[j].code).text(iso_countries[j].name));

                    var code = iso_countries[j].code;
                    var name = iso_countries[j].name;
                    $country_select.append($("<option data-image='static/public/plugins/ms-dropdown/images/icons/blank.gif' data-imagecss='flag " + code.toLowerCase() + "' data-title='" + name + "' />").val(code).text(name));
                    j++;
                });
                $country_select.val('US');
                $country_select.msDropdown({visibleRows: 10});

                keywordSearch.keywordFormWasSubmitted = false;
            },
            initSingleAppDetails: function () {
                singleAppScrapper.singleAppFormWasSubmitted = false;
                singleAppScrapper.mLocalStorage.restoreAppResultsUi();
            },
            initTopAppsSearch: function () {
                // filter keyword search result
                var filterTopsAppsSearchDebounce = debounce(function (input, e) {
                    var code = e.keyCode || e.which;
                    // tab=9, shift=16, ctrl=17 , arrows =37..40
                    if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                    if (code == '27') $(input).val(null);
                    // same word in all filters inputs
                    $('.filter-topapps-list').val($(input).val());
                    topAppsScrapping.filterTopAppsSearch($(input).val());
                }, 250);

                $('.filter-topapps-list').keyup(function (e) {
                    filterTopsAppsSearchDebounce(this, e);
                });

                // show/hide backup filter when scrolling keyword search
                $(window).scroll(debounce(function () {

                    if (!$('#scrapper_content').is(':visible') || !$('#scrapper-topapps-data').hasClass('active')) {
                        return;
                    }

                    var filterDiv = $('#topapps-search-filter');
                    var filterScrollDiv = $('#topapps-search-filter-scroll');
                    var filterInput = $('#topapps-filter-in');
                    var filterScrollInput = $('#topapps-filter-in-scroll');

                    if (filterDiv.isvisible(true)) {
                        if (!filterScrollInput.is(":focus")) {
                            filterScrollDiv.hide();
                        }
                    } else {
                        filterScrollDiv.show();
                        if (filterInput.is(":focus")) {
                            filterScrollInput.focus();
                        }
                    }
                }, 250));

                // Observe the list of apps so we can update the counter
                var target = document.getElementById('list-result-topapps-search');
                var observer = new MutationObserver(function (mutations) {
                    mutations.forEach(function (mutation) {
                        var nbItems = $("ul#list-result-topapps-search").children().length;
                        if (nbItems > 1) {
                            $('#topapps-search-filter').show();
                        } else {
                            $('#topapps-search-filter').hide();
                        }
                    });
                });
                var config = { childList: true};
                observer.observe(target, config);


                var $language_select = $('#topapps_language_select');
                $language_select.append("<option hidden disabled value selected>-- select a language --</option>");
                $.each(google_description_languages, function (i) {
                    var code = google_description_languages[i].code;
                    var language = google_description_languages[i].language;
                    var flag = google_description_languages[i].flag.replace(".png", "");
                    $language_select.append($("<option data-image='static/public/plugins/ms-dropdown/images/icons/blank.gif' data-imagecss='flag " + flag + "' data-title='" + language + "' />").val(code).text(language));
                });
                $language_select.val('en-US');
                $language_select.msDropdown({visibleRows: 10});


                var $country_select = $("#topapps_country_select");
                $country_select.append("<option hidden disabled value>-- select a country --</option>");
                $.each(iso_countries, function (j) {
                    var code = iso_countries[j].code;
                    var name = iso_countries[j].name;
                    $country_select.append($("<option data-image='static/public/plugins/ms-dropdown/images/icons/blank.gif' data-imagecss='flag " + code.toLowerCase() + "' data-title='" + name + "' />").val(code).text(name));
                });
                $country_select.val('US');
                $country_select.msDropdown({visibleRows: 10});


                var $collection_select = $("#topapps_collection_select");
                $.each(google_app_collections, function (k) {
                    $collection_select.append($("<option />").val(google_app_collections[k].param).text(google_app_collections[k].label));
                });
                $collection_select.val('topselling_free');

                /*
                 var category_select = $("#topapps_category_select");
                 //category_select.append("<option hidden disabled value>-- select a category --</option>");
                 $.each(google_app_categories, function (l) {
                 category_select.append($("<option />").val(google_app_categories[l].param).text(google_app_categories[l].label));
                 });
                 category_select.val('ALL');
                 */

                var category_select = $("#topapps_category_select");
                var optgroup_application = $("<optgroup label='Applications'/>");
                var optgroup_game = $("<optgroup label='Games'/>");
                var optgroup_family = $("<optgroup label='Family'/>");

                $.each(google_app_categories_APPLICATION, function (index) {
                    optgroup_application.append($("<option />").val(google_app_categories_APPLICATION[index].param).text(google_app_categories_APPLICATION[index].label));
                });

                $.each(google_app_categories_GAME, function (index) {
                    optgroup_game.append($("<option />").val(google_app_categories_GAME[index].param).text(google_app_categories_GAME[index].label));
                });

                $.each(google_app_categories_FAMILY, function (index) {
                    optgroup_family.append($("<option />").val(google_app_categories_FAMILY[index].param).text(google_app_categories_FAMILY[index].label));
                });

                category_select.append($("<option />").val('ALL').text('ALL'));
                category_select.append(optgroup_application);
                category_select.append(optgroup_game);
                category_select.append(optgroup_family);

                category_select.val('ALL');


                topAppsScrapping.topAppsFormWasSubmitted = false;
            },
            initFavoriteApps: function () {
                // filter keyword search result
                var filterDebounce = debounce(function (input, e) {
                    var code = e.keyCode || e.which;
                    // tab=9, shift=16, ctrl=17 , arrows =37..40
                    if (['9', '16', '17', '37', '38', '39', '40'].indexOf(code) > -1) return;
                    if (code == '27') $(input).val(null);
                    // same word in all filters inputs
                    $('.filter-favoriteapps-list').val($(input).val());
                    favoriteApps.filterFavoriteApps($(input).val());
                }, 250);

                $('.filter-favoriteapps-list').keyup(function (e) {
                    filterDebounce(this, e);
                });

                // show/hide backup filter when scrolling keyword search
                $(window).scroll(debounce(function () {

                    if (!$('#scrapper_content').is(':visible') || !$('#scrapper-favorites-data').hasClass('active')) {
                        return;
                    }

                    var filterDiv = $('#favoriteapps-search-filter');
                    var filterScrollDiv = $('#favoriteapps-search-filter-scroll');
                    var filterInput = $('#favoriteapps-filter-in');
                    var filterScrollInput = $('#favoriteapps-filter-in-scroll');

                    if (filterDiv.isvisible(true)) {
                        if (!filterScrollInput.is(":focus")) {
                            filterScrollDiv.hide();
                        }
                    } else {
                        filterScrollDiv.show();
                        if (filterInput.is(":focus")) {
                            filterScrollInput.focus();
                        }
                    }
                }, 250));

                favoriteApps.initFavoriteAppsTab();
            },
            initAppCountries: function () {
                appCountries.appCountriesFormWasSubmitted = false;
                appCountries.initAppCountriesTab();
            }
        };


        ScrapperApi.initUi = function () {
            getInstance();
            ui.initUi();
        };
        ScrapperApi.resetKeywordScrapperForm = function (e) {
            keywordSearch.resetKeywordScrapperForm(e);
        };
        ScrapperApi.clearKeywordResults = function (e) {
            keywordSearch.clearKeywordResults(e);
        };
        ScrapperApi.submitKeywordScrapperForm = function (e) {
            keywordSearch.submitKeywordScrapperForm(e);
        };
        ScrapperApi.loadMoreResult = function (button) {
            keywordSearch.loadMoreResult(button);
        };
        ScrapperApi.submitSingleAppScrapperForm = function (e) {
            singleAppScrapper.submitSingleAppScrapperForm(e);
        };
        ScrapperApi.getAppASO = function (e, packageName, typeAso) {
            general.getAppASO(e, packageName, typeAso);
        };
        ScrapperApi.getAppDetails = function (e, packageName) {
            singleAppScrapper.getAppDetails(e, packageName);
        };
        ScrapperApi.downloadApk = function (e, packageName) {
            general.downloadApk(e, packageName);
        };
        ScrapperApi.openApkDownloadPage = function (e, packageName) {
            general.openApkDownloadPage(e, packageName);
        };



        ScrapperApi.installOnDevice = function (e, packageName, appname) {
            general.installOnDevice(e, packageName, appname);
        };
        ScrapperApi.addAppToFavorite = function (e, packageName) {
            favoriteApps.addAppToFavorite(e, packageName);
        };
        ScrapperApi.removeAppFromFavorite = function (e, packageName) {
            favoriteApps.removeAppFromFavorite(e, packageName);
        };
        ScrapperApi.openDetailsAppSearch = function (button) {
            singleAppScrapper.openDetailsAppSearch(button);
        };
        ScrapperApi.deleteAppSearch = function (e, packageName, deleteAppSearch) {
            singleAppScrapper.deleteAppSearch(e, packageName, deleteAppSearch);
        };
        ScrapperApi.downloadAppGraphics = function (e) {
            singleAppScrapper.downloadAppGraphics(e);
        };
        ScrapperApi.hideVideoFrameCaption = function () {
            singleAppScrapper.hideVideoFrameCaption();
        };
        ScrapperApi.cancelSingleAppScrapping = function () {
            singleAppScrapper.cancelSingleAppScrapping();
        };
        ScrapperApi.openGPAppPage = function (event, language_code) {
            general.openGPAppPage(event, language_code)
        };
        ScrapperApi.confirmKwSearchWithoutProxy = function () {
            keywordSearch.confirmKwSearchWithoutProxy();
        };
        ScrapperApi.openProxySettings = function () {
            general.openProxySettings();
        };
        ScrapperApi.confirmSingleAppSearchWithoutProxy = function () {
            singleAppScrapper.confirmSingleAppSearchWithoutProxy();
        };
        ScrapperApi.getQuotaInfoAppDetails = function (e) {
            singleAppScrapper.getQuotaInfoAppDetails(e);
        };
        ScrapperApi.resetTopAppsForm = function (e) {
            topAppsScrapping.resetTopAppsForm(e);
        };
        ScrapperApi.clearTopAppsResults = function (e) {
            topAppsScrapping.clearTopAppsResults(e);
        };
        ScrapperApi.submitTopAppsForm = function (e) {
            topAppsScrapping.submitTopAppsForm(e);
        };
        ScrapperApi.confirmTopAppsSearchWithoutProxy = function () {
            topAppsScrapping.confirmTopAppsSearchWithoutProxy();
        };
        ScrapperApi.loadMoreTopAppsResult = function (button) {
            topAppsScrapping.loadMoreTopAppsResult(button);
        };
        ScrapperApi.submitAppCountriesForm = function (event) {
            appCountries.submitAppCountriesForm(event);
        };
        ScrapperApi.confirmAppCountriesWithoutProxy = function () {
            appCountries.confirmAppCountriesWithoutProxy();
        };
        ScrapperApi.checkAppCountries = function (e, appId) {
            appCountries.checkAppCountries(e, appId);
        };
        ScrapperApi.removeAppCountries = function (e, uuid) {
            appCountries.removeAppCountries(e, uuid);
        };
        ScrapperApi.cancelAppCountriesScrapping = function () {
            appCountries.cancelAppCountriesScrapping();
        };
        ScrapperApi.getQuotaInfoAppCounty = function (e) {
            appCountries.getQuotaInfoAppCounty(e);
        };
        ScrapperApi.webSocketMessagingProtocol = function (msg) {
            webSocketWrapper.webSocketMessagingProtocol(msg);
        };

        return ScrapperApi;
    };

    exports.MScrapper = MScrapper;
    window.ScrapperModule = new MScrapper;
}(this, jQuery);

$(function () {
    ScrapperModule.initUi();
});