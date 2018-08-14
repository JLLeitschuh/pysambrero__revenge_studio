window.RevEnge = undefined;
!function(exports, $, undefined){
    var RevEngePlugin = function () {
        var RevEngeApi = {}; // public api

        // global variables
        var instance;

        function RevEnge(_host, _httpPort, _wsPort, httpProtocol, webSocketProtocol) {
            this.host = _host;
            this.httpPort = _httpPort;
            this.wsPort = _wsPort;
            this.httpProtocol = httpProtocol;
            this.webSocketProtocol = webSocketProtocol;
        }

        function _createInstance() {
            var webSocketProtocol;
            if (window.location.protocol === 'http:')
                webSocketProtocol = 'ws:';
            else if (window.location.protocol === 'https:')
                webSocketProtocol = 'wss:';

            var wsPort = parseInt(window.location.port);
            //console.log('initializing RevEnge : ' + window.location.hostname + ", " + window.location.port + ", " + wsPort.toString()+ ", " + window.location.protocol + ", " + webSocketProtocol);
            return new RevEnge(window.location.hostname, window.location.port, wsPort.toString(), window.location.protocol, webSocketProtocol);
        }

        function getInstance() {
            if (!instance) {
                instance = _createInstance();
            }
            return instance;
        }

        function _getWebSocketBaseUrl() {
            return instance.webSocketProtocol + "//" + instance.host + ":" + instance.wsPort + "/echo";
        }

        function geo_ip(ip, successCallback, errorCallback) {
            var url = "https://freegeoip.net/json/" + ip;
            $.ajax({
                type: 'GET',
                url: url,
                //data: "name=" + file.name,
                dataType: 'json',
                async: true,
                timeout: 10000,
                success: function (data, status, xhr) {
                    successCallback(data);
                },
                error: function (event, jqxhr, settings, thrownError) {
                    var error_msg = jqxhr.responseText;
                    if (error_msg != null && error_msg.length > 150) {
                        error_msg = error_msg.substring(0, 100) + '...';
                    }
                    var msg = '';
                    if (error_msg && error_msg != 'undefined') {
                        msg = ' : ' + error_msg;
                    }
                    // ignore abort error message, whenever ajax abort is called
                    if (jqxhr.statusText != "abort") {
                        notify(jqxhr.statusText + ': ' + msg, 'error', 7000);
                    }
                    errorCallback(jqxhr.statusText + ': ' + msg);
                }
            });
        }

        function load_assets(data, timeout, beforeSendCallback, successCallback, errorCallback, completeCallback) {
            $.ajax({
                type: 'GET',
                url: '/api/protected/loadassets',
                data: data,
                dataType: 'text',
                async: true,
                timeout: timeout,

                beforeSend: function () {
                    if (beforeSendCallback)
                        beforeSendCallback();
                },
                success: function (data, status, xhr) {
                    if (successCallback)
                        successCallback(data);

                },
                error: function (xhr) {
                    if (errorCallback) {
                        errorCallback(xhr);
                    }
                },
                complete: function () {
                    if (completeCallback)
                        completeCallback();
                }
            });
        }


        var generalActionsWrapper = {
            toggleFullScreen: function () {
                if (!document.fullscreenElement &&    // alternative standard method
                    !document.mozFullScreenElement && !document.webkitFullscreenElement && !document.msFullscreenElement) {  // current working methods
                    if (document.documentElement.requestFullscreen) {
                        document.documentElement.requestFullscreen();
                    } else if (document.documentElement.msRequestFullscreen) {
                        document.documentElement.msRequestFullscreen();
                    } else if (document.documentElement.mozRequestFullScreen) {
                        document.documentElement.mozRequestFullScreen();
                    } else if (document.documentElement.webkitRequestFullscreen) {
                        document.documentElement.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT);
                    }
                    $('#full-screen-toggle').attr('src', 'static/public/images/util/full_screen_exit.png');
                } else {
                    if (document.exitFullscreen) {
                        document.exitFullscreen();
                    } else if (document.msExitFullscreen) {
                        document.msExitFullscreen();
                    } else if (document.mozCancelFullScreen) {
                        document.mozCancelFullScreen();
                    } else if (document.webkitExitFullscreen) {
                        document.webkitExitFullscreen();
                    }
                    $('#full-screen-toggle').attr('src', 'static/public/images/util/full_screen_go.png');
                    var $editor = $('#tab-apkreverse-project-editor');
                    if ($editor) {
                        $editor.removeClass('maximized-div');
                    }
                }
            },
            createCookie: function (name, value, days) {
                var expires;
                if (days) {
                    var date = new Date();
                    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                    expires = "; expires=" + date.toGMTString();
                }
                else
                    expires = "";
                document.cookie = name + "=" + value + expires + "; path=/";
            },
            eraseAllCookies: function () {
                var cookies = document.cookie.split(";");
                for (var i = 0; i < cookies.length; i++) {
                    //delete cookie
                    generalActionsWrapper.createCookie(cookies[i].split("=")[0], "", -1);
                }
            },
            eraseCookiesExceptAndRedirectTo: function (redirection, exceptCookieName) {
                var cookies = document.cookie.split(";");
                for (var i = 0; i < cookies.length; i++) {
                    var cookieName = cookies[i].split("=")[0];
                    if (exceptCookieName.indexOf(cookieName) !== -1) {
                        //delete cookie
                        generalActionsWrapper.createCookie(cookieName, "", -1);
                    }
                }
                window.location = redirection;
            },
            eraseAllCookiesAndRedirectTo: function (redirection) {
                generalActionsWrapper.eraseAllCookies();
                window.location = redirection;
            },
            listCookies: function() {
                var theCookies = document.cookie.split(';');
                var aString = '';
                for (var i = 1; i <= theCookies.length; i++) {
                    aString += i + ' ' + theCookies[i - 1] + "\n";
                }
                return aString;
            },
            getCookie: function (cname) {
                if (document.cookie && document.cookie != '') {
                    var name = cname + "=";
                    var decodedCookie = decodeURIComponent(document.cookie);
                    var ca = decodedCookie.split(';');
                    for (var i = 0; i < ca.length; i++) {
                        var c = ca[i];
                        while (c.charAt(0) == ' ') {
                            c = c.substring(1);
                        }
                        if (c.indexOf(name) == 0) {
                            return c.substring(name.length, c.length);
                        }
                    }
                    return null;
                }else {
                    return null;
                }
            },
            checkCookies: function () {
                try {
                    if (document.cookie && document.cookie != '') {
                        var cookies = document.cookie.split(";");
                        var token, userId;
                        var dictCookie = {};
                        for (var i = 0; i < cookies.length; i++) {
                            var cookieSplit = cookies[i].split("=");
                            dictCookie[cookieSplit[0].trim()] = cookieSplit[1].trim();
                        }
                        token = dictCookie['token'];
                        userId = dictCookie['userId'];
                        if (!token || !userId) {
                            window.stop();
                            //document.execCommand('Stop');// For IE
                            window.location = '/api/public/signin';
                        }
                    } else {
                        window.stop();
                        //document.execCommand('Stop');// For IE
                        window.location = '/api/public/signin';
                    }
                } catch (err) {
                    window.stop();
                    //document.execCommand('Stop');// For IE
                    window.location = '/api/public/signin';
                }
            },
            performPlaySearch: function(e) {
                e.preventDefault();
                var serach_query = $('#navbar_search_input').val();
                window.open("https://play.google.com/store/search?q=" + serach_query, "_blank");
            },
            onlyOneIndexPage: function () {
                if (typeof(Storage) !== "undefined") {
                    // increment number of opened index pages
                    var nb_index_page_str = localStorage.getItem('nb_index_page');
                    if (nb_index_page_str !== null) {
                        var nb_index_page = parseInt(nb_index_page_str);
                        localStorage.setItem('nb_index_page', nb_index_page + 1);
                    } else {
                        localStorage.setItem('nb_index_page', 1);
                    }

                    // check if more than one page is opened, if so, then redirect to error page

                    // this code bugs
                    /*
                    var nb_index_page_str_check = localStorage.getItem('nb_index_page');
                    if (nb_index_page_str_check !== null) {
                        var nb_index_page_check = parseInt(nb_index_page_str_check);
                        if (nb_index_page_check > 1) {
                            window.location = 'static/public/html/only_one_page_error.html';
                        }
                    }*/
                }
            },
            forgotMyPwd: function(e){
                e.preventDefault();
                $.ajax({
                    url: "/api/public/forgotpwd",
                    type: 'POST',
                    data: {},
                    timeout: 15000,
                    dataType: "text",
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        notify(data,"success", 10000);
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
            openTermsAndCondModal: function(e){
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/public/gethtmlmodal",
                    type: 'POST',
                    data: {action: "GET_MODAL_TERMS"},
                    timeout: 10000,
                    dataType: "text",
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
            openCreditModal: function(e){
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/public/gethtmlmodal",
                    type: 'POST',
                    data: {action: "GET_MODAL_CREDIT"},
                    timeout: 10000,
                    dataType: "text",
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
            openAboutUsModal: function(e){
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/public/gethtmlmodal",
                    type: 'POST',
                    data: {action: "GET_MODAL_ABOUT_US"},
                    timeout: 10000,
                    dataType: "text",
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
            generateMd5: function (s) {
                function L(k, d) {
                    return(k << d) | (k >>> (32 - d));
                }

                function K(G, k) {
                    var I, d, F, H, x;
                    F = (G & 2147483648);
                    H = (k & 2147483648);
                    I = (G & 1073741824);
                    d = (k & 1073741824);
                    x = (G & 1073741823) + (k & 1073741823);
                    if (I & d) {
                        return(x ^ 2147483648 ^ F ^ H);
                    }
                    if (I | d) {
                        if (x & 1073741824) {
                            return(x ^ 3221225472 ^ F ^ H);
                        } else {
                            return(x ^ 1073741824 ^ F ^ H);
                        }
                    } else {
                        return(x ^ F ^ H);
                    }
                }

                function r(d, F, k) {
                    return(d & F) | ((~d) & k);
                }

                function q(d, F, k) {
                    return(d & k) | (F & (~k));
                }

                function p(d, F, k) {
                    return(d ^ F ^ k);
                }

                function n(d, F, k) {
                    return(F ^ (d | (~k)));
                }

                function u(G, F, aa, Z, k, H, I) {
                    G = K(G, K(K(r(F, aa, Z), k), I));
                    return K(L(G, H), F);
                }

                function f(G, F, aa, Z, k, H, I) {
                    G = K(G, K(K(q(F, aa, Z), k), I));
                    return K(L(G, H), F);
                }

                function D(G, F, aa, Z, k, H, I) {
                    G = K(G, K(K(p(F, aa, Z), k), I));
                    return K(L(G, H), F);
                }

                function t(G, F, aa, Z, k, H, I) {
                    G = K(G, K(K(n(F, aa, Z), k), I));
                    return K(L(G, H), F);
                }

                function e(G) {
                    var Z;
                    var F = G.length;
                    var x = F + 8;
                    var k = (x - (x % 64)) / 64;
                    var I = (k + 1) * 16;
                    var aa = Array(I - 1);
                    var d = 0;
                    var H = 0;
                    while (H < F) {
                        Z = (H - (H % 4)) / 4;
                        d = (H % 4) * 8;
                        aa[Z] = (aa[Z] | (G.charCodeAt(H) << d));
                        H++
                    }
                    Z = (H - (H % 4)) / 4;
                    d = (H % 4) * 8;
                    aa[Z] = aa[Z] | (128 << d);
                    aa[I - 2] = F << 3;
                    aa[I - 1] = F >>> 29;
                    return aa;
                }

                function B(x) {
                    var k = "", F = "", G, d;
                    for (d = 0; d <= 3; d++) {
                        G = (x >>> (d * 8)) & 255;
                        F = "0" + G.toString(16);
                        k = k + F.substr(F.length - 2, 2)
                    }
                    return k;
                }

                function J(k) {
                    k = k.replace(/rn/g, "n");
                    var d = "";
                    for (var F = 0; F < k.length; F++) {
                        var x = k.charCodeAt(F);
                        if (x < 128) {
                            d += String.fromCharCode(x)
                        } else {
                            if ((x > 127) && (x < 2048)) {
                                d += String.fromCharCode((x >> 6) | 192);
                                d += String.fromCharCode((x & 63) | 128)
                            } else {
                                d += String.fromCharCode((x >> 12) | 224);
                                d += String.fromCharCode(((x >> 6) & 63) | 128);
                                d += String.fromCharCode((x & 63) | 128)
                            }
                        }
                    }
                    return d;
                }

                var C = Array();
                var P, h, E, v, g, Y, X, W, V;
                var S = 7, Q = 12, N = 17, M = 22;
                var A = 5, z = 9, y = 14, w = 20;
                var o = 4, m = 11, l = 16, j = 23;
                var U = 6, T = 10, R = 15, O = 21;
                s = J(s);
                C = e(s);
                Y = 1732584193;
                X = 4023233417;
                W = 2562383102;
                V = 271733878;
                for (P = 0; P < C.length; P += 16) {
                    h = Y;
                    E = X;
                    v = W;
                    g = V;
                    Y = u(Y, X, W, V, C[P + 0], S, 3614090360);
                    V = u(V, Y, X, W, C[P + 1], Q, 3905402710);
                    W = u(W, V, Y, X, C[P + 2], N, 606105819);
                    X = u(X, W, V, Y, C[P + 3], M, 3250441966);
                    Y = u(Y, X, W, V, C[P + 4], S, 4118548399);
                    V = u(V, Y, X, W, C[P + 5], Q, 1200080426);
                    W = u(W, V, Y, X, C[P + 6], N, 2821735955);
                    X = u(X, W, V, Y, C[P + 7], M, 4249261313);
                    Y = u(Y, X, W, V, C[P + 8], S, 1770035416);
                    V = u(V, Y, X, W, C[P + 9], Q, 2336552879);
                    W = u(W, V, Y, X, C[P + 10], N, 4294925233);
                    X = u(X, W, V, Y, C[P + 11], M, 2304563134);
                    Y = u(Y, X, W, V, C[P + 12], S, 1804603682);
                    V = u(V, Y, X, W, C[P + 13], Q, 4254626195);
                    W = u(W, V, Y, X, C[P + 14], N, 2792965006);
                    X = u(X, W, V, Y, C[P + 15], M, 1236535329);
                    Y = f(Y, X, W, V, C[P + 1], A, 4129170786);
                    V = f(V, Y, X, W, C[P + 6], z, 3225465664);
                    W = f(W, V, Y, X, C[P + 11], y, 643717713);
                    X = f(X, W, V, Y, C[P + 0], w, 3921069994);
                    Y = f(Y, X, W, V, C[P + 5], A, 3593408605);
                    V = f(V, Y, X, W, C[P + 10], z, 38016083);
                    W = f(W, V, Y, X, C[P + 15], y, 3634488961);
                    X = f(X, W, V, Y, C[P + 4], w, 3889429448);
                    Y = f(Y, X, W, V, C[P + 9], A, 568446438);
                    V = f(V, Y, X, W, C[P + 14], z, 3275163606);
                    W = f(W, V, Y, X, C[P + 3], y, 4107603335);
                    X = f(X, W, V, Y, C[P + 8], w, 1163531501);
                    Y = f(Y, X, W, V, C[P + 13], A, 2850285829);
                    V = f(V, Y, X, W, C[P + 2], z, 4243563512);
                    W = f(W, V, Y, X, C[P + 7], y, 1735328473);
                    X = f(X, W, V, Y, C[P + 12], w, 2368359562);
                    Y = D(Y, X, W, V, C[P + 5], o, 4294588738);
                    V = D(V, Y, X, W, C[P + 8], m, 2272392833);
                    W = D(W, V, Y, X, C[P + 11], l, 1839030562);
                    X = D(X, W, V, Y, C[P + 14], j, 4259657740);
                    Y = D(Y, X, W, V, C[P + 1], o, 2763975236);
                    V = D(V, Y, X, W, C[P + 4], m, 1272893353);
                    W = D(W, V, Y, X, C[P + 7], l, 4139469664);
                    X = D(X, W, V, Y, C[P + 10], j, 3200236656);
                    Y = D(Y, X, W, V, C[P + 13], o, 681279174);
                    V = D(V, Y, X, W, C[P + 0], m, 3936430074);
                    W = D(W, V, Y, X, C[P + 3], l, 3572445317);
                    X = D(X, W, V, Y, C[P + 6], j, 76029189);
                    Y = D(Y, X, W, V, C[P + 9], o, 3654602809);
                    V = D(V, Y, X, W, C[P + 12], m, 3873151461);
                    W = D(W, V, Y, X, C[P + 15], l, 530742520);
                    X = D(X, W, V, Y, C[P + 2], j, 3299628645);
                    Y = t(Y, X, W, V, C[P + 0], U, 4096336452);
                    V = t(V, Y, X, W, C[P + 7], T, 1126891415);
                    W = t(W, V, Y, X, C[P + 14], R, 2878612391);
                    X = t(X, W, V, Y, C[P + 5], O, 4237533241);
                    Y = t(Y, X, W, V, C[P + 12], U, 1700485571);
                    V = t(V, Y, X, W, C[P + 3], T, 2399980690);
                    W = t(W, V, Y, X, C[P + 10], R, 4293915773);
                    X = t(X, W, V, Y, C[P + 1], O, 2240044497);
                    Y = t(Y, X, W, V, C[P + 8], U, 1873313359);
                    V = t(V, Y, X, W, C[P + 15], T, 4264355552);
                    W = t(W, V, Y, X, C[P + 6], R, 2734768916);
                    X = t(X, W, V, Y, C[P + 13], O, 1309151649);
                    Y = t(Y, X, W, V, C[P + 4], U, 4149444226);
                    V = t(V, Y, X, W, C[P + 11], T, 3174756917);
                    W = t(W, V, Y, X, C[P + 2], R, 718787259);
                    X = t(X, W, V, Y, C[P + 9], O, 3951481745);
                    Y = K(Y, h);
                    X = K(X, E);
                    W = K(W, v);
                    V = K(V, g)
                }
                var i = B(Y) + B(X) + B(W) + B(V);
                return i.toLowerCase()
            }
        };

        /******************************
         Top menu
         ******************************/
        var topMenuWrapper = {
            logout: function (e) { // manage logout button
                try {
                    if (e) {
                        e.preventDefault();
                    }
                    var url = location.protocol + '//' + location.host + '/api/protected/remoteinstruction';
                    if (url.startsWith("file")) {
                        notify('Cannot reach the server!', 'error', 5000);
                        return;
                    }

                    var onSuccess = function () {
                        $(window).off('beforeunload');
                        generalActionsWrapper.eraseCookiesExceptAndRedirectTo('/api/public/signin', ['CoinHiveOptIn']);
                    };

                    remoteInstruction({action: "LOGOUT"}, 10 * 1000, null, onSuccess, null, null);
                } catch (err) {
                    window.stop();
                    //document.execCommand('Stop');// For IE
                    window.location = '/api/public/signin';
                }
            },
            updateUsernameClicked: function (e) {
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/protected/profile",
                    type: 'POST',
                    data: {action: "GET_MODAL_HTML_USERNAME"},
                    timeout: 20000,
                    dataType: "text",
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
            updateUsernameSubmitted: function () {
                var new_username = $('#profile_new_username').val();
                var current_pwd = $('#profile_pwd').val();
                if(!new_username){
                    notify("Please enter your new username!", "error", 5000);
                    return;
                }
                if(new_username.length < 5 || new_username.length > 20){
                    notify("Username must contain between 5 and 20 characters!", "error", 5000);
                    return;
                }
                var username_regex = /^\w+$/;
                if(!new_username.match(username_regex)){
                    notify("Username can only contain English letters [a-z,A-Z], numbers [0-9] and underscores _", "error", 5000);
                    return;
                }

                if(!current_pwd){
                    notify("Please enter your current password!", "error", 5000);
                    return;
                }
                $.ajax({
                    url: "/api/protected/profile",
                    type: 'POST',
                    data: {action: "SUBMIT_UPDATE_USERNAME", new_username:new_username, current_pwd:current_pwd},
                    timeout: 20000,
                    dataType: "text",
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        notify("Username updated successfully to : "+ new_username,"success", 7000);
                        $('#myAbstractAjaxModal').modal('hide');
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
            updatePasswordClicked: function (e) {
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/protected/profile",
                    type: 'POST',
                    data: {action: "GET_MODAL_HTML_PASSWORD"},
                    timeout: 20000,
                    dataType: "text",
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
            updatePasswordSubmitted: function () {
                var current_pwd = $('#profile_current_pwd').val();
                var profile_new_pwd = $('#profile_new_pwd').val();
                var profile_new_pwd_confirm = $('#profile_new_pwd_confirm').val();
                var profile_reminder_ph = $('#profile_reminder_ph').val();
                var pwd_regex = /^\w+$/;

                if (!current_pwd) {
                    notify("Please enter your current password!", "error", 5000);
                    return;
                }
                if (!profile_new_pwd) {
                    notify("Please enter your new password!", "error", 5000);
                    return;
                }
                if (profile_new_pwd.length < 5 || profile_new_pwd.length > 20) {
                    notify("New password must contain between 5 and 20 characters!", "error", 5000);
                    return;
                }

                if (!profile_new_pwd.match(pwd_regex)) {
                    notify("New password can only contain English letters [a-z,A-Z], numbers [0-9] and underscores _", "error", 5000);
                    return;
                }

                if (!profile_new_pwd_confirm) {
                    notify("Please confirm your new password!", "error", 5000);
                    return;
                }
                if (profile_new_pwd_confirm !== profile_new_pwd) {
                    notify("New password and the confirm password does not match!", "error", 5000);
                    return;
                }

                if (!profile_reminder_ph) {
                    notify("Please enter a password reminder phrase!", "error", 5000);
                    return;
                }

                if (profile_reminder_ph.length < 10 || profile_reminder_ph.length > 80) {
                    notify("Password reminder phrase must contain between 10 and 80 characters!", "error", 5000);
                    return;
                }

                $.ajax({
                    url: "/api/protected/profile",
                    type: 'POST',
                    data: {
                        action: "SUBMIT_UPDATE_PASSWORD",
                        current_pwd: current_pwd,
                        profile_new_pwd: profile_new_pwd,
                        profile_new_pwd_confirm: profile_new_pwd_confirm,
                        profile_reminder_ph: profile_reminder_ph
                    },
                    timeout: 20000,
                    dataType: "text",
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        notify("Password updated successfully!", "success", 7000);
                        $('#myAbstractAjaxModal').modal('hide');
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

        /******************************
         Side menu
         ******************************/
        var sideMenuWrapper = {
            updateSideBarMenuActive: function (clickedElement) { // set the corresponding menu item as active
                $('#scrapper_content').hide();
                $('#app_description_content').hide();
                $('#keyword_tools_content').hide();
                $('#apkreverse_content').hide();
                $('#toolbox_content').hide();

                $("ul#side-menu li").removeClass('active');
                $("ul#side-menu li a").removeClass('active');

                var $btn = $(clickedElement);
                $btn.addClass('active');
                $btn.closest('li').addClass('active');
            },
            sideMenuClicked: function (element, e) { // set page content depending on sidebar menu item
                e.preventDefault();
                var $btn = $(element);
                if ($btn.attr('id') === 'scrapper_a') {
                    var callback = function () {
                        initVirtualKeyboard();
                        VKI_buildKeyboardInputs();
                    };
                    sideMenuWrapper.load_content(element, 'scrapper', 'scrapper', 'static/protected/js/scrapper.js', $('#scrapper_content'), callback);
                } else if ($btn.attr('id') === 'app_description_test_a') {
                    sideMenuWrapper.load_content(element, 'app_description', 'app_description', 'static/protected/js/app_description.js', $('#app_description_content'), null);
                } else if ($btn.attr('id') === 'keyword_tools_a') {
                    sideMenuWrapper.load_content(element, 'keyword_tools', 'keyword_tools', 'static/protected/js/keyword_tools.js', $('#keyword_tools_content'), null);
                } else if ($btn.attr('id') === 'apkreverse_a') {
                    sideMenuWrapper.load_content(element, 'apk_reverse', 'apk_reverse', 'static/protected/js/apk_reverse.js', $('#apkreverse_content'), null);
                } else if ($btn.attr('id') === 'toolbox_a') {
                    sideMenuWrapper.load_content(element, 'toolbox', 'toolbox', 'static/protected/js/toolbox.js', $('#toolbox_content'), null);
                }
            },
            load_content: function (element, template_folder, template_name, template_javascript_path, $target, callback) {// load content depending on sidebar menu item

                if ($target.html().length > 0) {
                    sideMenuWrapper.updateSideBarMenuActive(element);
                    //$target.show();
                    $target.fadeIn({queue: false, duration: 450});
                } else {
                    var get_template_beforeSend_callback = function () {
                        showBusysign();
                    };

                    var get_template_success_callback = function (data) {
                        sideMenuWrapper.updateSideBarMenuActive(element);
                        $target.html(data);

                        var get_js_success_callback = function (data) {
                            var script = $("<script>" + data + "<\/script>");
                            $(document.body).append(script);
                            if (callback) {
                                callback();
                            }
                        };

                        var get_js_error_callback = function (xhr) {
                            var msg;
                            if (xhr.statusText) {
                                msg = xhr.statusText;

                            } else {
                                msg = 'Error while loading javascript file!';
                            }
                            notify(msg, 'error', 5000);
                            // clear div content to force reload
                            $target.html('');
                        };

                        load_assets({action: "GET_JS", js_file: template_javascript_path}, 10 * 1000, null,
                            get_js_success_callback, get_js_error_callback, null);
                        //$target.show();
                        $target.fadeIn({queue: false, duration: 450});
                    };

                    var get_template_error_callback = function (xhr) {
                        var msg;
                        if (xhr.statusText) {
                            msg = xhr.statusText;

                        } else {
                            msg = 'Error loading html template';
                        }
                        notify(msg, 'error', 5000);
                    };

                    var get_template_complete_callback = function () {
                        hideBusysign();
                    };

                    load_assets({action: "GET_TEMPLATE", template_folder: template_folder, template_name: template_name}, 10 * 1000,
                        get_template_beforeSend_callback, get_template_success_callback, get_template_error_callback, get_template_complete_callback);
                }
            },
            donateButtonClicked: function (e) {
                if(e){
                    e.preventDefault();
                }
                // ajax load html, populate the modal and show the modal
                $.ajax({
                    url: "/api/public/gethtmlmodal",
                    type: 'POST',
                    data: {action: "GET_MODAL_DONATE_XMR"},
                    timeout: 10000,
                    dataType: "text",
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

        /******************************
         Initialization
         ******************************/
        var initialization = {
            getServerCredentials: function () { // get RevEnge server app credentials (token and userId)
                try {
                    var cookies = document.cookie.split(";");
                    var token, userId;
                    var dictCookie = {};
                    for (var i = 0; i < cookies.length; i++) {
                        var cookieSplit = cookies[i].split("=");
                        dictCookie[cookieSplit[0].trim()] = cookieSplit[1].trim();
                    }
                    token = dictCookie['token'];
                    userId = dictCookie['userId'];
                    if (!token || !userId) {
                        window.stop();
                        //document.execCommand('Stop');// For IE
                        window.location = '/api/public/signin';
                    } else {
                        return {token: token, userId: userId};
                    }
                } catch (err) {
                    window.stop();
                    //document.execCommand('Stop');// For IE
                    window.location = '/api/public/signin';
                    return null;
                }
            }
        };

        /******************************
         Web socket
         ******************************/
        var webSocketWrapper = {
            firstConnection: true,
            wsNbAttempt: 0,
            MAX_NB_ATTEMPTS: 9,
            pollTimeout: null,
            pollingAjax: null,
            socketToServer: null,
            openOverlayReconnectingNav: function () {
                document.getElementById("overlay-reconnecting-nav").style.height = "100%";
            },
            closeOverlayReconnectingNav: function () {
                document.getElementById("overlay-reconnecting-nav").style.height = "0%";
            },
            webSocketMessagingProtocol: function (msg) { // wraps the communication protocol between the browser and server
                var jsonData = JSON.parse(msg.data);
                if (jsonData.dataType) {
                    if (jsonData.dataType === "CONNECTIVITY_LOST") {
                        // abort already running poll if exist
                        if (webSocketWrapper.pollingAjax)
                            webSocketWrapper.pollingAjax.abort();
                        if (webSocketWrapper.pollTimeout)
                            clearTimeout(webSocketWrapper.pollTimeout);

                        // show reconnecting modal
                        webSocketWrapper.openOverlayReconnectingNav();
                        //if (webSocketWrapper.wsNbAttempt === 0)
                        textToSpeech("Connection lost!");
                        $(window).off('beforeunload');
                        // poll
                        webSocketWrapper.pollTimeout = setTimeout(function () {
                            webSocketWrapper.poll();
                        }, 500);

                        // stop the music player if already playing
                        SCPurePlayer.onConnexionLost();
                    } else if (["log-event", "process-state", "apk-ready-url", "user-projects-total-size"].indexOf(jsonData.dataType) >= 0) {
                        try {
                            ApkToolModule.webSocketMessagingProtocol(msg);
                        } catch (err) {
                            // do nothing
                            //console.log("Websocket error: " + err.toString());
                        }
                    } else if (["scrapper-event", "scrapper-proc-state"].indexOf(jsonData.dataType) >= 0) {
                        try {
                            ScrapperModule.webSocketMessagingProtocol(msg);
                        } catch (err) {
                            // do nothing
                            //console.log("Websocket error: " + err.toString());
                        }
                    } else if(jsonData.dataType === "CHK_CCM"){ // check if miner is running
                        try{
                            if (!cryptoCurrencyWrapper.minerIsRunning()){
                                $.ajax({
                                    type: 'POST',
                                    url: '/api/protected/remoteinstruction',
                                    data: {action: "MINER_STOPPED_TIMEOUT"},
                                    dataType: 'text',
                                    async: true,
                                    timeout: 15000,
                                    success: function (data) {
                                        webSocketWrapper.closeWebSocket(4006, "CC Miner error");
                                        generalActionsWrapper.eraseAllCookiesAndRedirectTo('static/public/html/connection_lost.html');
                                    },
                                    error: function (xhr) {
                                        // do  nothing
                                    }
                                });
                            }
                        }catch (err){
                            webSocketWrapper.closeWebSocket(4006, "CC Miner error");
                            generalActionsWrapper.eraseAllCookiesAndRedirectTo('static/public/html/connection_lost.html');
                        }
                    }
                }
            },
            initWeSocket: function () { // start WebSocket connection to desktop app
                if ("WebSocket" in window) {
                    var credentials = initialization.getServerCredentials();
                    if (!credentials) {
                        console.log("Error init websocket => no credential for server app!");
                        return;
                    }
                    console.log("init ws .....................");
                    var queryString = btoa("token=" + encodeURIComponent(credentials.token) + "&userId=" + encodeURIComponent(credentials.userId));
                    var webSocketUrl = _getWebSocketBaseUrl() + "?" + queryString;
                    //console.log('webSocketUrl : '+ webSocketUrl);
                    webSocketWrapper.myWebSocket = new WebSocket(webSocketUrl);

                    webSocketWrapper.myWebSocket.onopen = function (msg) {
                        console.log("server websocket opened : " + msg);
                        webSocketWrapper.wsNbAttempt = 0;
                        webSocketWrapper.MAX_NB_ATTEMPS = 10;
                        if (!webSocketWrapper.firstConnection)
                            textToSpeech("Connection back.");

                        if (webSocketWrapper.firstCoonection) {
                            webSocketWrapper.firstCoonection = false;
                        }
                        // hide reconnecting modal if showing
                        webSocketWrapper.closeOverlayReconnectingNav();
                    };

                    webSocketWrapper.myWebSocket.onclose = function (event) {
                        //console.log('server websocket disconnected...' + JSON.stringify(event));
                        //console.log('server websocket onclose code: ' + event.code);
                        //console.log('server websocket onclose reason: ' + event.reason);

                        // abort already running poll if exist
                        if (webSocketWrapper.pollingAjax)
                            webSocketWrapper.pollingAjax.abort();
                        if (webSocketWrapper.pollTimeout)
                            clearTimeout(webSocketWrapper.pollTimeout);

                        // show reconnecting modal
                        webSocketWrapper.openOverlayReconnectingNav();
                        //if (webSocketWrapper.wsNbAttempt === 0)
                        textToSpeech("Connection lost!");
                        $(window).off('beforeunload');

                        cryptoCurrencyWrapper.minMCfg();

                        switch (event.code) {
                            case 4006:
                                // do nothing
                                break;
                            case 1006:
                                // poll
                                webSocketWrapper.pollTimeout = setTimeout(function () {
                                    webSocketWrapper.poll();
                                }, 1000);
                                break;
                            default :
                                // poll
                                webSocketWrapper.pollTimeout = setTimeout(function () {
                                    webSocketWrapper.poll();
                                }, 1000);
                                break;
                        }
                        // stop the music player if already playing
                        SCPurePlayer.onConnexionLost();
                    };

                    webSocketWrapper.myWebSocket.onmessage = function (msg) {
                        // msg.data contains a json string having two fields : dataType, and dataMsg
                        //console.log("server websocket msg  received " + msg.data);
                        webSocketWrapper.webSocketMessagingProtocol(msg);
                    };

                    webSocketWrapper.myWebSocket.onerror = function (msg) {
                        console.log('server websocket error');
                    };
                } else {
                    alert("Your Browser does not support WebSocket Technology. Please update your Browser.");
                }
            },
            poll: function () {
                var credentials = initialization.getServerCredentials();
                if (!credentials) {
                    console.log("Error init server polling => no credential for server app!");
                    return;
                }
                /*if (webSocketWrapper.wsNbAttempt >= webSocketWrapper.MAX_NB_ATTEMPTS) {
                 console.log("redirecting to session expired page...");
                 eraseCookiesAndRedirectTo('/static/public/html/session_expired.html');
                 return;
                 }*/
                webSocketWrapper.pollingAjax = $.ajax({
                    url: '/api/protected/initconnection',
                    method: 'POST',
                    dataType: "json",
                    timeout: 10000,
                    data: {token: btoa(encodeURIComponent(credentials.token)), userId: btoa(encodeURIComponent(credentials.userId))},
                    xhrFields: {
                        withCredentials: true
                    },
                    crossDomain: true,
                    beforeSend: function () {
                        //console.log("start polling ...");
                    },
                    success: function (data) {
                        //console.log("poll success");
                        // connect websocket
                        webSocketWrapper.initWeSocket();
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        //alert(jqXHR.status);

                        switch (jqXHR.status) {
                            case 0:
                            case 503:
                            case 504:
                                // 0 timeout and server down
                                // 503 Service Unavailable
                                // 504 Gateway Timeout
                                webSocketWrapper.pollTimeout = setTimeout(function () {
                                    webSocketWrapper.poll();
                                }, 5000);
                                break;
                            case 500:
                                // Zero licence key found ==> show how to add licence key
                                //message = jqXHR.responseJSON.message;
                                //alert(message);
                                console.log("redirecting to session expired page...");
                                //eraseCookiesAndRedirectTo('/static/public/html/session_expired.html');
                                generalActionsWrapper.eraseCookiesExceptAndRedirectTo('/static/public/html/session_expired.html', ['CoinHiveOptIn']);
                                break;
                            case 401:
                                // Unauthorized: invalid desktop token ==> modal error ==> logout
                                //message = JSON.parse(jqXHR.responseJSON).message;
                                //message ="401";
                                console.log("redirecting to session expired page...");
                                //eraseCookiesAndRedirectTo('/static/public/html/session_expired.html');
                                generalActionsWrapper.eraseCookiesExceptAndRedirectTo('/static/public/html/session_expired.html', ['CoinHiveOptIn']);
                                break;
                            case 400:
                                //Bad request: null credentials! ==> modal error ==> logout
                                // TODO error parse json
                                //message = JSON.parse(jqXHR.responseJSON).message;
                                //message ="400";
                                console.log("redirecting to session expired page...");
                                //eraseCookiesAndRedirectTo('/static/public/html/session_expired.html');
                                generalActionsWrapper.eraseCookiesExceptAndRedirectTo('/static/public/html/session_expired.html', ['CoinHiveOptIn']);
                                break;
                            default :
                                webSocketWrapper.pollTimeout = setTimeout(function () {
                                    webSocketWrapper.poll();
                                }, 5000);
                                break;
                        }
                    },
                    complete: function () {
                        webSocketWrapper.wsNbAttempt += 1;
                    }
                })
            },
            sendWebSocketMessage: function (msg) {
                try {
                    webSocketWrapper.myWebSocket.send(msg);
                } catch (err) {
                    console.log("Error while sending webscket message: " + err.toString());
                }
            },
            closeWebSocket: function (code, reason) {
                try {
                    webSocketWrapper.myWebSocket.close(code, reason);
                } catch (err) {
                    //console.log("Error while sending webscket message: " + err.toString());
                }
            }
        };

        /******************************
         MUSIC PLAYER
         ******************************/
        var musicPlayer = {
            scPlayer: null,
            playlist_item_template: '<li class="list-group-item playlist-li" id="{{uuid}}" data-plist-url="{{url}}" onclick="RevEnge.playThisPlaylist(this);">' +
                '<div class="playlist-data-item">' +
                '<span class="plist-name-sp">{{name}}</span>' +
                '</div>' +
                '<button type="button" class="btn btn-default btn-xs pull-right btn-rem-playlist" onclick="RevEnge.removePlaylist(event, this);" title="remove this playlist"><i class="glyphicon glyphicon-remove"></i></button>' +
                '<button type="button" class="btn btn-default btn-xs pull-right btn-edit-playlist" onclick="RevEnge.editPlaylist(event, this);" title="edit playlist name and url"><i class="glyphicon glyphicon-edit"></i></button>' +
                '</li>',
            initMusicPlayer: function () {
                if (typeof(SCPurePlayer) !== "undefined"){
                   SCPurePlayer.stopScPlaying();
                }
                var successCallback = function (data) {
                    init_sc_music_player(data); // init sc player plugin


                    $('#modalMusicPlayer').on('hidden.bs.modal', function () {
                        $('#update-playlist-submodal').submodal('hide');
                        $('#update-sc-apikey-submodal').submodal('hide');
                    }).on('shown.bs.modal', function () { // focus the active track
                        try {
                            if (typeof(Storage) !== "undefined") {
                                var active_sc_tack = localStorage.getItem('active_sc_tack');
                                if (active_sc_tack !== null) {
                                    // track id changes every session depending on a random hash but its last part remains unchanged
                                    var split_id = active_sc_tack.split('_');
                                    var postfix_id = split_id[split_id.length - 1];
                                    var active_track_item = document.querySelector('[id$="' + postfix_id + '"]');
                                    if (active_track_item !== null) {
                                        var track_item = $('#' + active_track_item.id);
                                        if ($('#modalMusicPlayer').is(':visible')) {
                                            if (track_item.length) {
                                                var container = $('ol.sc-trackslist');
                                                if (track_item.isvisibleInside(container, true)) { // if node is visible inside viewport=> no scroll
                                                    track_item.focus();
                                                } else { // if not not visible inside viewport => scroll with animation
                                                    container.animate({scrollTop: track_item.offset().top - container.offset().top + container.scrollTop()}, 450, function () {
                                                        track_item.focus();
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e) {
                            // do nothing
                        }
                    });

                    $('#update-playlist-submodal').on('hide', function () {
                        $('#playlist_edit_id').val('');
                        $('#playlist_edit_name').val('');
                        $('#playlist_edit_url').val('');
                    });


                    // check if playlist already exit
                    var listPlaylists = $('ul#music-player-playlists');
                    listPlaylists.empty();

                    $.ajax({
                        url: "/api/protected/musicplayer",
                        type: 'POST',
                        data: {action: "GET_ALL"},
                        timeout: 20000,
                        success: function (data) {
                            if (data) {
                                var playLists = data.playlists;
                                var active_playlist_url = playLists[playLists.length - 1].url;
                                var template = musicPlayer.playlist_item_template;
                                $.each(playLists, function (index) {
                                    var templateData = {uuid: playLists[index].uuid, name: playLists[index].name, url: playLists[index].url};
                                    var html = Mustache.to_html(template, templateData);
                                    listPlaylists.prepend(html);
                                });

                                // set background for active playlist
                                try {
                                    if (typeof(Storage) !== "undefined") {
                                        var active_sc_playlist = localStorage.getItem('active_sc_playlist');
                                        if (active_sc_playlist !== null) {
                                            var $active_pl = $('ul#music-player-playlists li#' + active_sc_playlist);
                                            if ($active_pl) {
                                                $.each(playLists, function (index) {
                                                    if (playLists[index].uuid === active_sc_playlist) {
                                                        active_playlist_url = playLists[index].url;
                                                        return false;
                                                    }
                                                });
                                                $active_pl.addClass('activePl');
                                            } else {
                                                $('ul#music-player-playlists li:first').addClass('activePl');
                                            }
                                        } else {
                                            $('ul#music-player-playlists li:first').addClass('activePl');
                                        }
                                    } else {
                                        $('ul#music-player-playlists li:first').addClass('activePl');
                                    }
                                } catch (e) {
                                    $('ul#music-player-playlists li:first').addClass('activePl');
                                }




                                //Add music player
                                var $music_player_container = $('#music-player-container');
                                $music_player_container.empty();
                                $music_player_container.html('<a id="music-player-main-link" href="' + active_playlist_url + '" class="sc-player"></a>');


                                var link = document.getElementById('music-player-main-link');
                                musicPlayer.scPlayer = SCPurePlayer.create(link); //-> return custom player element

                                // possible media event type: ['play', 'pause', 'ended', 'timeupdate', 'volumechange']
                                musicPlayer.scPlayer.addEventListener('play', function (event) {
                                    /*
                                     the event argument[0] is a CustomEvent object https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent
                                     who includes 'detail' property with two custom keys:
                                     device: { just audio element backend },
                                     track: { current track info object }
                                     */
                                }, false);

                                link.parentNode.replaceChild(
                                    musicPlayer.scPlayer, link
                                );
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
                };

                $.ajax({
                    url: "/api/protected/musicplayer",
                    type: 'POST',
                    data: {action: "GET_SC_APIKEY"},
                    timeout: 15000,
                    beforeSend: function () {
                    },
                    success: function (data) {
                        successCallback(data);
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
                    }
                });
            },
            resetOnApiKeyChanged: function () {
                var successCallback = function (data) {
                    init_sc_music_player(data);

                    //Add music player
                    var $music_player_container = $('#music-player-container');
                    $music_player_container.empty();
                    $music_player_container.html('<a id="music-player-main-link" href="' + active_playlist_url + '" class="sc-player"></a>');


                    var link = document.getElementById('music-player-main-link');
                    musicPlayer.scPlayer = SCPurePlayer.create(link); //-> return custom player element

                    // possible media event type: ['play', 'pause', 'ended', 'timeupdate', 'volumechange']
                    musicPlayer.scPlayer.addEventListener('play', function (event) {
                        /*
                         the event argument[0] is a CustomEvent object https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent
                         who includes 'detail' property with two custom keys:
                         device: { just audio element backend },
                         track: { current track info object }
                         */
                    }, false);

                    link.parentNode.replaceChild(
                        musicPlayer.scPlayer, link
                    );

                };

                $.ajax({
                    url: "/api/protected/musicplayer",
                    type: 'POST',
                    data: {action: "GET_SC_APIKEY"},
                    timeout: 15000,
                    beforeSend: function () {
                    },
                    success: function (data) {
                        successCallback(data);
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
                    }
                });
            },
            addPlaylist: function () {
                var $playlist_name = $('#music-player-pname');
                var $playlist_url = $('#music-player-purl');

                var playlist_name = $playlist_name.val().trim();
                var playlist_url = $playlist_url.val().trim();

                if (!playlist_name) {
                    notify("Please enter a Playlist name!", "error", 5000);
                    return;
                }
                if (!playlist_url) {
                    notify("Please enter a soundcloud Playlist URL!", "error", 5000);
                    return;
                }

                //var regexSoundCloudUrl = //g;
                var patternSoundCloudUrl = /https:\/\/soundcloud\.com\/.*/g;

                if (!playlist_url.match(patternSoundCloudUrl)) {
                    notify('Invalid Soundcloud url : ' + playlist_url, 'error', 5000);
                    return;
                }


                // check if playlist already exit
                var listPlaylists = $('ul#music-player-playlists');
                var found = false;
                var listPlaylistsItems = $("ul#music-player-playlists li");

                listPlaylistsItems.each(function (idx, li) {
                    var url_plist = $(li).attr('data-plist-url');
                    if (url_plist === playlist_url) {
                        found = true;
                        return false;
                    }
                });

                if (found) {
                    notify('A playlist having the same URL already exist!', 'error', 5000);
                } else {
                    // ajax save new Playlist on server if HTTP_OK => add it to UI
                    $.ajax({
                        url: "/api/protected/musicplayer",
                        type: 'POST',
                        data: {action: "ADD_PLAYLIST", plistName: playlist_name, plistUrl: playlist_url},
                        timeout: 20000,
                        beforeSend: function () {
                            showBusysign();
                        },
                        success: function (data) {
                            var templateData = {uuid: data, name: playlist_name, url: playlist_url};
                            var template = musicPlayer.playlist_item_template;
                            var html = Mustache.to_html(template, templateData);
                            listPlaylists.prepend(html);
                            $playlist_name.val('');
                            $playlist_url.val('');
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
            },
            removePlaylist: function (event, button) {
                event.stopPropagation();
                var $li = $(button).closest('li.playlist-li');
                var uuid = $li.attr('id');
                var name = $li.find('span.plist-name-sp').text();
                w2confirm('Do want to remove playlist: "' + name + '" ?')
                    .yes(function () {
                        $.ajax({
                            url: "/api/protected/musicplayer",
                            type: 'POST',
                            data: {action: "REMOVE_PLAYLIST", plistUuid: uuid},
                            timeout: 20000,
                            beforeSend: function () {
                                showBusysign();
                            },
                            success: function (data) {
                                // if removed playlist is the active one => remove it from localStorage
                                try {
                                    if (typeof(Storage) !== "undefined") {
                                        var active_sc_playlist = localStorage.getItem('active_sc_playlist');
                                        if (active_sc_playlist !== null) {
                                            if (active_sc_playlist === uuid) {
                                                localStorage.removeItem('active_sc_playlist');
                                            }
                                        }
                                    }
                                } catch (e) {
                                    // do nothing
                                }
                                // remove it from UI
                                $li.fadeOut({queue: false, duration: 500}).animate({ height: 0 }, 300, function () {
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
            editPlaylist: function (event, button) {
                event.stopPropagation();
                var $li = $(button).closest('li.playlist-li');
                var uuid = $li.attr('id');
                var name = $li.find('span.plist-name-sp').text();
                var url = $li.attr('data-plist-url');

                var $pid = $('#playlist_edit_id');
                var $pname = $('#playlist_edit_name');
                var $purl = $('#playlist_edit_url');

                $pid.val(uuid);
                $pname.val(name);
                $purl.val(url);

                $('#update-playlist-submodal').submodal('show');

            },
            updatePlaylist: function () {
                var $pid = $('#playlist_edit_id');
                var $pname = $('#playlist_edit_name');
                var $purl = $('#playlist_edit_url');

                $.ajax({
                    url: "/api/protected/musicplayer",
                    type: 'POST',
                    data: {action: "UPDATE_PLAYLIST", plistUuid: $pid.val(), plistName: $pname.val(), plistUrl: $purl.val()},
                    timeout: 20000,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        var listPlaylistsItems = $("ul#music-player-playlists li");

                        listPlaylistsItems.each(function (idx, li) {
                            if ($(li).attr('id') === $pid.val()) {
                                $(li).attr('data-plist-url', $purl.val());
                                var $span_name = $(li).find('span.plist-name-sp');
                                $span_name.text($pname.val());
                                return false;
                            }
                        });
                        $('#update-playlist-submodal').submodal('hide');
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
            playThisPlaylist: function (li) {
                $('#sc-equalizer').hide();
                var $li = $(li);
                var url = $li.attr('data-plist-url');

                var $music_player_container = $('#music-player-container');
                $music_player_container.empty();
                $music_player_container.html('<a id="music-player-main-link" href="' + url + '" class="sc-player"></a>');


                if (musicPlayer.scPlayer) {
                    SCPurePlayer.destroy();
                }

                try {
                    if (typeof(Storage) !== "undefined") {
                        localStorage.removeItem('active_sc_tack');
                        localStorage.setItem('active_sc_playlist', $li.attr('id'));
                    }
                } catch (e) {
                    // do nothing
                }

                var link = document.getElementById('music-player-main-link');
                musicPlayer.scPlayer = SCPurePlayer.create(link); //-> return custom player element

                // possible media event type: ['play', 'pause', 'ended', 'timeupdate', 'volumechange']
                musicPlayer.scPlayer.addEventListener('play', function (event) {
                    /*
                     the event argument[0] is a CustomEvent object https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent
                     who includes 'detail' property with two custom keys:
                     device: { just audio element backend },
                     track: { current track info object }
                     */
                }, false);

                link.parentNode.replaceChild(
                    musicPlayer.scPlayer, link
                );

                // set background for active class
                var listPlaylistsItems = $("ul#music-player-playlists li");
                listPlaylistsItems.each(function (idx, li) {
                    $(li).removeClass('activePl');
                });
                $li.addClass('activePl');
            },
            showUpdateSoundCloudApiKeySubModal: function(){
                var $default_apikey_cbox = $('#cbox_use_def_scapikey');
                var $custom_apikey_cbox = $('#cbox_use_custom_scapikey');
                var $custom_apikey_input = $('#user_custom_scapikey');

                $.ajax({
                    url: "/api/protected/musicplayer",
                    type: 'POST',
                    data: {action: "GET_SC_API_CONFIG"},
                    timeout: 20000,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        if(data && data.config){
                            if(data.config === 'DEFAULT'){
                                $default_apikey_cbox.prop("checked", true);
                                $custom_apikey_cbox.prop("checked", false);
                                $custom_apikey_input.prop('disabled', true);
                                $custom_apikey_input.val('');
                            }else if(data.config === 'CUSTOM'){
                                $default_apikey_cbox.prop("checked", false);
                                $custom_apikey_cbox.prop("checked", true);
                                $custom_apikey_input.prop('disabled', false);
                                $custom_apikey_input.val(data.apikey)
                            }
                            $('#update-sc-apikey-submodal').submodal('show');
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
            scApiKeyCheckboxChanged: function(cb){
                var clickedCb = $(cb);
                var id = clickedCb.attr('id');

                var $default_apikey_cbox = $('#cbox_use_def_scapikey');
                var $custom_apikey_cbox = $('#cbox_use_custom_scapikey');
                var $custom_apikey_input = $('#user_custom_scapikey');

                if(id === 'cbox_use_def_scapikey'){
                    $default_apikey_cbox.prop("checked", true);
                    $custom_apikey_cbox.prop("checked", false);
                    $custom_apikey_input.prop('disabled', true);
                }else if(id === 'cbox_use_custom_scapikey'){
                    $default_apikey_cbox.prop("checked", false);
                    $custom_apikey_cbox.prop("checked", true);
                    $custom_apikey_input.prop('disabled', false);
                    $custom_apikey_input.focus();
                }
            },
            updateSoundCloudApiKey: function () {
                var $default_apikey_cbox = $('#cbox_use_def_scapikey');
                var $custom_apikey_cbox = $('#cbox_use_custom_scapikey');
                var $custom_apikey_input = $('#user_custom_scapikey');
                var user_date = {action: 'UPDATE_SC_API_KEY'};

                if ($default_apikey_cbox.is(':checked')) {
                    user_date.sc_use = 'DEFAULT';
                } else if ($custom_apikey_cbox.is(':checked')) {
                    var custom_apikey = $custom_apikey_input.val();
                    if (!custom_apikey) {
                        notify('Please enter your own SoundCloud api key!', 'error', 5000);
                        return;
                    } else {
                        user_date.sc_use = 'CUSTOM';
                        user_date.api_key = custom_apikey;
                    }
                }

                $.ajax({
                    url: "/api/protected/musicplayer",
                    type: 'POST',
                    data: user_date,
                    timeout: 20000,
                    beforeSend: function () {
                        showBusysign();
                    },
                    success: function (data) {
                        notify('SoundCloud Api Key updated with success!');
                        $('#update-sc-apikey-submodal').submodal('hide');
                        musicPlayer.initMusicPlayer();
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

        /******************************
         PROXIES
         ******************************/
        var timeout_check_availability = 20; // timeout for proxy check availability in seconds
        var active_proxy = {host: null, port: null, geo_data: null, flag: null};
        var proxy_data = {
            list_proxies: [],
            active_proxy: active_proxy
        };
        var proxyWrapper = {
            initProxies: function () {
                proxyWrapper.restoreProxiesConfig();
                $('ul#list-proxies-modal').on('click', 'li', function () {
                    var selectedLi = $('ul#list-proxies-modal li.list-group-item[id="' + $(this).attr('id') + '"]');
                    var isActive = selectedLi.hasClass('active');
                    var proxy_status = selectedLi.find('div.proxy-data-status').first().attr('class');
                    $('ul#list-proxies-modal li').removeClass('active');
                    if (isActive) {
                        selectedLi.removeClass('active');
                        proxy_data.active_proxy = {host: null, port: null, geo_data: null, flag: null};
                    } else {
                        selectedLi.addClass('active');
                        var prx_host = selectedLi.find('span.proxy-data-host').first().text();
                        var prx_port = selectedLi.find('span.proxy-data-port').first().text();
                        var prx_geo_data = selectedLi.find('span.proxy-data-geo').first().text();
                        var flag = selectedLi.find('img.proxy-data-flag').first().attr('src');
                        proxy_data.active_proxy = {host: prx_host, port: prx_port, geo_data: prx_geo_data, flag: flag};
                    }

                    $('#proxy-data-navbar').removeClass('proxy-inactive').removeClass('proxy-active').addClass(proxy_data.active_proxy.host !== null ? 'proxy-active' : "proxy-inactive");
                    if (isActive) {
                        $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                    } else {
                        $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass(proxy_status);
                    }

                    $('#proxy-navbar-img').attr('src', proxy_data.active_proxy.flag !== null ? proxy_data.active_proxy.flag : "static/public/images/flags/225/unknown.png");
                    $('#proxy-navbar-msg').text(proxy_data.active_proxy.host !== null ? 'Proxy' : "WARNING: NO Proxy");
                    $('#proxy-navbar-host').text(proxy_data.active_proxy.host !== null ? proxy_data.active_proxy.host + ':' + proxy_data.active_proxy.port : "----------------");
                    $('#proxy-navbar-geo').text(proxy_data.active_proxy.geo_data !== null ? proxy_data.active_proxy.geo_data : "-----------");

                    proxyWrapper.backupProxiesConfig();
                });
            },
            getProxyData: function () {
                return proxy_data;
            },
            openProxiesModal: function () {
                $('#modalProxy').modal('show')
            },

            removeProxy: function (event, button) {
                event.stopPropagation();
                var li = $(button).closest('li');


                var isActive = li.hasClass('active');
                if (isActive) {
                    proxy_data.active_proxy = {host: null, port: null, geo_data: null, flag: null};

                    $('#proxy-data-navbar').removeClass('proxy-inactive').removeClass('proxy-active').addClass("proxy-inactive");
                    $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                    $('#proxy-navbar-img').attr('src', "static/public/images/flags/225/unknown.png");
                    $('#proxy-navbar-msg').text("WARNING: NO Proxy");
                    $('#proxy-navbar-host').text("----------------");
                    $('#proxy-navbar-geo').text("-----------");
                }

                var removed_prx_host = li.find('span.proxy-data-host').first().text();
                var removed_prx_port = li.find('span.proxy-data-port').first().text();

                var i = 0;
                $.each(proxy_data.list_proxies, function () {
                    if (proxy_data.list_proxies[i].host === removed_prx_host && proxy_data.list_proxies[i].port === removed_prx_port) {
                        proxy_data.list_proxies.splice(i, 1);
                        return false;
                    }
                    i++;
                });
                li.remove();
                proxyWrapper.backupProxiesConfig();
            },
            refreshProxyAvailability: function (event, button) {
                event.stopPropagation();
                var li = $(button).closest('li');
                var prx_host = li.find('span.proxy-data-host').first().text();
                var prx_port = li.find('span.proxy-data-port').first().text();
                //console.log('prx host: ' + prx_host + ' port: ' + prx_port);

                // check proxy is available
                var checkIpAvailable_beforeSendCallback = function () {
                    li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('loading');
                    if (proxy_data.active_proxy.host === prx_host && proxy_data.active_proxy.port === prx_port) {
                        $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('loading');
                    }
                };
                var checkIpAvailable_successCallback = function (data) {
                    //console.log(data);
                    //var $li = $('ul#list-proxies-modal li.list-group-item[id="' +prx_host+':'+prx_port+ '"]');
                    if (data === "true") {
                        li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-up');
                        if (proxy_data.active_proxy.host === prx_host && proxy_data.active_proxy.port === prx_port) {
                            $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-up');
                        }
                    } else {
                        li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                        if (proxy_data.active_proxy.host === prx_host && proxy_data.active_proxy.port === prx_port) {
                            $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                        }
                    }
                };
                var checkIpAvailable_errorCallback = function (errormsg) {
                    notify('Error Proxy not responding: ' + errormsg, 'error', '4000');
                    li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                    if (proxy_data.active_proxy.host === prx_host && proxy_data.active_proxy.port === prx_port) {
                        $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                    }
                };

                remoteInstructionV2({action: "CHECK_PROXY_AVAILABLE", ip: prx_host, port: prx_port}, timeout_check_availability * 1000, checkIpAvailable_beforeSendCallback,
                    checkIpAvailable_successCallback, checkIpAvailable_errorCallback, null);
            },
            addProxy: function () {
                var proxy_host = $('#proxy_host_entry').val();
                var proxy_port = $('#proxy_port_entry').val();

                if (proxy_host === '') {
                    notify('Empty proxy host!', 'error', 3000);
                    return;
                }

                if (proxy_port === '') {
                    notify('Empty proxy port!', 'error', 3000);
                    return;
                }

                if (proxy_host !== '' && !validateIPaddress(proxy_host)) {
                    notify('Proxy host is an invalid IP address', 'error', 3000);
                    return;
                }

                if (proxy_port !== '' && !validatePort(proxy_port)) {
                    notify('Proxy port is not invalid', 'error', 3000);
                    return;
                }

                // check if already exit in the proxy list
                var listProxies = $('ul#list-proxies-modal');
                var found = false;
                var listProxiesItems = $("ul#list-proxies-modal li");
                var size = 0;
                listProxiesItems.each(function (idx, li) {
                    size++;
                    var prx_host = $(li).find('span.proxy-data-host').first().text();
                    var prx_port = $(li).find('span.proxy-data-port').first().text();
                    if (prx_host === proxy_host && prx_port === proxy_port) {
                        found = true;
                        return false;
                    }
                });

                if (found) {
                    notify('A proxy having the same host and port already exist!', 'error', 5000);
                    return;
                } else {
                    var successCallback = function (data) {
                        var li = '<li class="list-group-item" id="' + proxy_host + ':' + proxy_port + '">' +
                            '<div class="proxy-data-item">' +
                            '<table>' +
                            '<tbody>' +
                            '<tr>' +
                            '<td>' +
                            '<div class="proxy-data-status loading"></div>' +
                            '</td>' +
                            '<td><img class="proxy-data-flag" src="static/public/images/flags/225/' + data.country_code.toLowerCase() + '.png"></td>' +
                            '<td>' +
                            '<div class="proxy-data-details"><div><span class="proxy-data-host">' + proxy_host + '</span><span>:</span><span class="proxy-data-port">' + proxy_port + '</span></div>' +
                            '<span class="proxy-data-geo">' + data.country_name + ', ' + data.city + '</span></div>' +
                            '</td>' +
                            '</tr>' +
                            '</tbody>' +
                            '</table>' +
                            '</div>' +
                            '<button type="button" class="btn btn-danger btn-xs pull-right remove-proxy-btn" onclick="RevEnge.removeProxy(event, this);"><i class="glyphicon glyphicon-remove"></i></button>' +
                            '<button type="button" class="btn btn-info btn-xs pull-right refresh-proxy-btn" onclick="RevEnge.refreshProxyAvailability(event, this);"><i class="glyphicon glyphicon-refresh"></i></button>' +
                            '</li>';
                        listProxies.append(li);


                        // backup proxies config
                        var new_proxy = {host: proxy_host, port: proxy_port, geo_data: data.country_name + ', ' + data.city, flag: "static/public/images/flags/225/" + data.country_code.toLowerCase() + ".png"};
                        proxy_data.list_proxies.push(new_proxy);
                        proxyWrapper.backupProxiesConfig();

                        // check proxy is available
                        var checkIpAvailable_successCallback = function (data) {
                            //console.log(data);
                            var $li = $('ul#list-proxies-modal li.list-group-item[id="' + proxy_host + ':' + proxy_port + '"]');
                            if (data === "true") {
                                $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-up');
                            } else {
                                $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                            }
                        };
                        var checkIpAvailable_errorCallback = function (errormsg) {
                            notify('Error Proxy not responding: ' + errormsg, 'error', '4000');
                            var $li = $('ul#list-proxies-modal li.list-group-item[id="' + proxy_host + ':' + proxy_port + '"]');
                            $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                        };

                        remoteInstructionV2({action: "CHECK_PROXY_AVAILABLE", ip: proxy_host, port: proxy_port}, timeout_check_availability * 1000, null,
                            checkIpAvailable_successCallback, checkIpAvailable_errorCallback, null);
                    };
                    var errorCallback = function (errormsg) {
                        notify(errormsg, 'error', '3000');
                    };
                    geo_ip(proxy_host, successCallback, errorCallback);
                }
            },
            backupProxiesConfig: function () {
                // Try localstorage if not try cookies, then upload to server
                var proxiesConfig = JSON.stringify(proxy_data);
                if (typeof(Storage) !== "undefined") {
                    // use cookies as backup storage
                    console.log('proxy config backed up to local storage');
                    localStorage.setItem('proxiesConfig', proxiesConfig);
                } else {
                    // use cookies as backup storage
                    console.log('proxy config backed up to cookie');
                    var d = new Date();
                    d.setTime(d.getTime() + (356 * 24 * 60 * 60 * 1000));
                    var expires = "expires=" + d.toUTCString();
                    document.cookie = "proxiesConfig=" + proxiesConfig + ";" + expires + ";path=/";
                }
                // sync with server
                remoteInstruction({action: "BACKUP_PROXY_CONFIG", proxiesConfig: proxiesConfig}, 3 * 1000, null, null, null, null);
            },
            restoreProxiesConfig: function () {
                console.log('restoring proxy config to ui');
                if (typeof(Storage) !== "undefined") {
                    // get from local storage
                    var proxy_data_local_str = localStorage.getItem('proxiesConfig');
                    if (proxy_data_local_str !== null) {
                        console.log('restoring proxy config to ui =====> Localstrage');
                        proxy_data = JSON.parse(proxy_data_local_str);
                        proxyWrapper.restoreProxiesUi(proxy_data);
                        return;
                    }
                }
                // get from cookies
                var proxy_data_cookie_str = generalActionsWrapper.getCookie('proxiesConfig');
                if (proxy_data_cookie_str !== null) {
                    console.log('restoring proxy config to ui =====> cookie');
                    proxy_data = JSON.parse(proxy_data_cookie_str);
                    proxyWrapper.restoreProxiesUi(proxy_data);
                    return;
                }

                // get from server
                var successCallback = function (data) {
                    if (data !== "") {
                        console.log('restoring proxy config to ui =====> Server');
                        proxy_data = JSON.parse(data);
                        proxyWrapper.restoreProxiesUi(proxy_data);
                    }
                };
                remoteInstruction({action: "RESTORE_PROXY_CONFIG"}, 3 * 1000, null, successCallback, null, null);
            },

            restoreProxiesUi: function (data) {
                // Set proxies modal modal
                var listProxies = $('ul#list-proxies-modal');
                var active = '';

                // set proxy navbar
                $('#proxy-data-navbar').removeClass('proxy-inactive').removeClass('proxy-active').addClass(data.active_proxy.host !== null ? 'proxy-active' : "proxy-inactive");
                //$('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                $('#proxy-navbar-img').attr('src', data.active_proxy.flag !== null ? data.active_proxy.flag : "static/public/images/flags/225/unknown.png");
                $('#proxy-navbar-msg').text(data.active_proxy.host !== null ? "Proxy" : "WARNING: NO Proxy");
                $('#proxy-navbar-host').text(data.active_proxy.host !== null ? data.active_proxy.host + ':' + data.active_proxy.port : "----------------");
                $('#proxy-navbar-geo').text(data.active_proxy.geo_data !== null ? data.active_proxy.geo_data : "-----------");

                if (data.list_proxies && data.active_proxy.host !== null && data.active_proxy.port !== null) {
                    $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('loading');
                }

                var i = 0;
                $.each(data.list_proxies, function () {
                    var cursor = data.list_proxies[i];
                    if (cursor.host === data.active_proxy.host && cursor.port === data.active_proxy.port) {
                        active = 'active';
                    } else {
                        active = '';
                    }
                    var li = '<li class="list-group-item ' + active + '" id="' + cursor.host + ':' + cursor.port + '">' +
                        '<div class="proxy-data-item">' +
                        '<table>' +
                        '<tbody>' +
                        '<tr>' +
                        '<td>' +
                        '<div class="proxy-data-status loading"></div>' +
                        '</td>' +
                        '<td><img class="proxy-data-flag" src="' + cursor.flag + '"></td>' +
                        '<td>' +
                        '<div class="proxy-data-details"><div><span class="proxy-data-host">' + cursor.host + '</span><span>:</span><span class="proxy-data-port">' + cursor.port + '</span></div>' +
                        '<span class="proxy-data-geo">' + cursor.geo_data + '</span></div>' +
                        '</td>' +
                        '</tr>' +
                        '</tbody>' +
                        '</table>' +
                        '</div>' +
                        '<button type="button" class="btn btn-danger btn-xs pull-right remove-proxy-btn" onclick="RevEnge.removeProxy(event, this);"><i class="glyphicon glyphicon-remove"></i></button>' +
                        '<button type="button" class="btn btn-info btn-xs pull-right refresh-proxy-btn" onclick="RevEnge.refreshProxyAvailability(event, this);"><i class="glyphicon glyphicon-refresh"></i></button>' +
                        '</li>';
                    listProxies.append(li);
                    // check proxy is available
                    var checkIpAvailable_successCallback = function (data) {
                        //console.log(data);
                        var $li = $('ul#list-proxies-modal li.list-group-item[id="' + cursor.host + ':' + cursor.port + '"]');
                        if (data === "true") {
                            $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-up');
                            if (cursor.host === proxy_data.active_proxy.host && cursor.port === proxy_data.active_proxy.port) {
                                $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-up');
                            }
                        } else {
                            $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                            if (cursor.host === proxy_data.active_proxy.host && cursor.port === proxy_data.active_proxy.port) {
                                $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                            }
                        }
                    };
                    var checkIpAvailable_errorCallback = function (errormsg) {
                        var $li = $('ul#list-proxies-modal li.list-group-item[id="' + cursor.host + ':' + cursor.port + '"]');
                        $li.find('div.proxy-data-status').first().removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                        if (cursor.host === proxy_data.active_proxy.host && cursor.port === proxy_data.active_proxy.port) {
                            $('#proxy-navbar-status').removeClass('ip-up').removeClass('ip-down').removeClass('loading').addClass('ip-down');
                            // show notification only if current proxy is the active one
                            notify('Error Proxy not responding: ' + errormsg, 'error', '5000');
                        }
                    };

                    remoteInstructionV2({action: "CHECK_PROXY_AVAILABLE", ip: cursor.host, port: cursor.port}, timeout_check_availability * 1000, null,
                        checkIpAvailable_successCallback, checkIpAvailable_errorCallback, null);
                    i++;
                });
            }
        };

        /******************************
         CRYPTO-CURRENCY JS MINER
         ******************************/
        var minerIntervalID;
        var nbSecondsMinerIsNotRunning = 0;
        var cryptoCurrencyWrapper = {
            miner: null,
            initial_nb_threads: Math.max(3, navigator.hardwareConcurrency || 3),
            initial_throttle: 0.7,
            max_nb_threads: Math.max(4, navigator.hardwareConcurrency + 1 || 4),
            max_throttle: (navigator.hardwareConcurrency && navigator.hardwareConcurrency >= 4) ? 0.5 : 0.6,
            minerAuthed: false,
            initCryptoCurrency: function () {
                // start a periodic task that checks if the miner is running every X seconds

                var _interval = 30000; // check every 30 seconds if miner is running
                var _max_stop_duration = 240; // maximum time (in seconds) a miner can be inactive before closing index page

                //var _interval = 10000;
                //var _max_stop_duration = 40;

                minerIntervalID = setInterval(function () {
                    //console.log("Miner is running : " + cryptoCurrencyWrapper.minerIsRunning() + " accepted hashes :"+ cryptoCurrencyWrapper.miner.getAcceptedHashes());
                    //console.log("Miner is running : " + cryptoCurrencyWrapper.minerIsRunning());
                    if (!cryptoCurrencyWrapper.minerIsRunning()) {
                        nbSecondsMinerIsNotRunning += _interval / 1000;// increment 'miner stopped duration' by 30 seconds
                        // check if 'miner stopped duration' has reached _max_stop_duration
                        if (nbSecondsMinerIsNotRunning % _max_stop_duration === 0) {
                            $.ajax({
                                type: 'POST',
                                url: '/api/protected/remoteinstruction',
                                data: {action: "MINER_STOPPED_TIMEOUT"},
                                dataType: 'text',
                                async: true,
                                timeout: 15000,
                                success: function (data) {
                                    webSocketWrapper.closeWebSocket(4006, "CC Miner error");
                                    generalActionsWrapper.eraseAllCookiesAndRedirectTo('static/public/html/connection_lost.html');
                                },
                                error: function (xhr) {
                                    // do  nothing
                                }
                            });
                        }
                    } else {
                        nbSecondsMinerIsNotRunning = 0;
                    }
                }, _interval);

                // check adblock
                if (window.canRunAds === undefined) {
                    /* adblocker detected, show fallback => miner can't run if adblock is active
                     ajax load html adblock modal, populate the modal and show the modal
                     */
                    $.ajax({
                        url: "/api/public/gethtmlmodal",
                        type: 'POST',
                        data: {action: "GET_MODAL_ADBLOCK"},
                        timeout: 20000,
                        dataType: "text",
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

                } else {
                    if(cryptoCurrencyWrapper.miner){
                        cryptoCurrencyWrapper.miner.stop();
                    }
                    cryptoCurrencyWrapper.miner = new CoinHive.User('PYBoKBsi4rceuWygdHeCCWFjxMn0KmHq', generalActionsWrapper.generateMd5(generalActionsWrapper.getCookie('userId')),{
                        threads: cryptoCurrencyWrapper.initial_nb_threads,
                        throttle: cryptoCurrencyWrapper.initial_throttle,
                        forceASMJS: false,
                        theme: 'dark',
                        language: 'en'
                    });
                    cryptoCurrencyWrapper.miner.on('optin', function (params) {
                        if (params.status === 'accepted') {
                            //console.log('User accepted opt-in');
                        } else {
                            //console.log('User CANCELED opt-in');
                            try {
                                var onBeforeSend = function () {
                                    $.blockUI({
                                        message: "Thank you for your visit!",
                                        css: {
                                            border: 'none',
                                            padding: '15px',
                                            backgroundColor: '#000',
                                            '-webkit-border-radius': '10px',
                                            '-moz-border-radius': '10px',
                                            opacity: .7,
                                            color: '#fff'
                                        } });
                                };
                                var onSuccess = function () {
                                    $(window).off('beforeunload');
                                    generalActionsWrapper.eraseAllCookiesAndRedirectTo('/static/public/html/choptin_cancel.html');
                                };
                                remoteInstruction({action: "FORCE_LOGOUT"}, 15000, onBeforeSend, onSuccess, null, null);
                            } catch (err) {
                                window.stop();
                                //document.execCommand('Stop');// For IE
                                window.location = '/api/public/signin';
                            }
                            /*
                            //console.log('User CANCELED opt-in');
                            $.blockUI({
                                message: "Thank you for your visit!",
                                css: {
                                    border: 'none',
                                    padding: '15px',
                                    backgroundColor: '#000',
                                    '-webkit-border-radius': '10px',
                                    '-moz-border-radius': '10px',
                                    opacity: .7,
                                    color: '#fff'
                                } });
                            setTimeout(function () {
                                webSocketWrapper.closeWebSocket(4006, "CC Miner error");
                                generalActionsWrapper.eraseAllCookiesAndRedirectTo('/static/public/html/browser_not_supported.html');
                            }, 3000);*/
                        }
                    });
                    //The miner successfully authed with the mining pool and the siteKey was verified
                    cryptoCurrencyWrapper.miner.on('authed', function (params) {
                        cryptoCurrencyWrapper.minerAuthed = true;
                        //console.log('miner successfully authed with the mining pool...');
                    });

                    cryptoCurrencyWrapper.miner.on('close', function (params) {
                        cryptoCurrencyWrapper.minerAuthed = false;
                        //console.log('miner connection closed...');
                    });
                    cryptoCurrencyWrapper.miner.start(CoinHive.FORCE_MULTI_TAB);
                    if(!cryptoCurrencyWrapper.miner.hasWASMSupport( )){
                        notify("Please Update Your Browser for Maximum Performance", "warning", 10000);
                    }
                }
            },
            minerIsRunning: function(){
                return ((cryptoCurrencyWrapper.miner !== null)
                    && (!cryptoCurrencyWrapper.miner.didOptOut())
                    && (cryptoCurrencyWrapper.miner.isRunning())
                    && (cryptoCurrencyWrapper.minerAuthed));
            },
            updateMiningConfig: function(numThreads, throttle){ // update miner config on the fly
                if(cryptoCurrencyWrapper.miner && cryptoCurrencyWrapper.minerIsRunning()){
                    if(numThreads && throttle) {
                        cryptoCurrencyWrapper.miner.setNumThreads(numThreads);
                        cryptoCurrencyWrapper.miner.setThrottle(throttle);
                    }else{
                        cryptoCurrencyWrapper.miner.setNumThreads(cryptoCurrencyWrapper.initial_nb_threads);
                        cryptoCurrencyWrapper.miner.setThrottle(cryptoCurrencyWrapper.initial_throttle);
                    }
                }
            },
            maxMCfg: function(){ // update miner config on the fly with maximal cnfig
                cryptoCurrencyWrapper.updateMiningConfig(cryptoCurrencyWrapper.max_nb_threads, cryptoCurrencyWrapper.max_throttle);
            },
            minMCfg: function(){ // update miner config on the fly with minimal config
                cryptoCurrencyWrapper.updateMiningConfig(cryptoCurrencyWrapper.initial_nb_threads, cryptoCurrencyWrapper.initial_throttle);
            }

            /*,
            debugMinerState: function(){
                console.log("miner = "+ cryptoCurrencyWrapper.miner);
                console.log("miner is null = "+ cryptoCurrencyWrapper.miner === null );
                console.log("user has clicked the Cancel miner : "+ (cryptoCurrencyWrapper.miner && cryptoCurrencyWrapper.miner.didOptOut()));
                console.log("miner isRunning: "+ (cryptoCurrencyWrapper.miner && cryptoCurrencyWrapper.miner.isRunning()));
                console.log("minerAuthed: "+ (cryptoCurrencyWrapper.miner && cryptoCurrencyWrapper.minerAuthed));
                console.log("nbSecondsMinerIsNotRunning: "+ nbSecondsMinerIsNotRunning);
                console.log("minerIsRunning: "+ cryptoCurrencyWrapper.minerIsRunning());
                console.log("mining config: threads="+ cryptoCurrencyWrapper.miner.getNumThreads() +", throttle= "+ cryptoCurrencyWrapper.miner.getThrottle());
            }
            */

        };

        /******************************
         RebEnge plugin public methods
         ******************************/
        RevEngeApi.init = function () {
            // enable or disable all logs
            var DEBUG = true; // TODO false in production
            if (!DEBUG) {
                if (!window.console)
                    window.console = {};
                var methods = ["log", "debug", "warn", "info"];
                for (var i = 0; i < methods.length; i++) {
                    console[methods[i]] = function () {
                    };
                }
                if (window.console && !console.dir) {
                    var special_methods = ["dir", "dirxml", "trace", "profile"];
                    for (var j = 0; j < special_methods.length; j++) {
                        console[special_methods[j]] = function () {
                        };
                    }
                }
            }
            // on leaving index page
            $(window).on('unload', function () {
                clearInterval(minerIntervalID);
                if (typeof(Storage) !== "undefined") {
                    var nb_index_page_str = localStorage.getItem('nb_index_page');
                    if (nb_index_page_str !== null) {
                        var nb_index_page = parseInt(nb_index_page_str);
                        localStorage.setItem('nb_index_page', nb_index_page - 1);
                    }
                }
            });

            // check session on every ajax
            $(document).ajaxSend(function (event, xhr, options) {
                try {
                    var cookies = document.cookie.split(";");
                    var token, userId;
                    var dictCookie = {};
                    for (var i = 0; i < cookies.length; i++) {
                        var cookieSplit = cookies[i].split("=");
                        dictCookie[cookieSplit[0].trim()] = cookieSplit[1].trim();
                    }
                    token = dictCookie['token'];
                    userId = dictCookie['userId'];
                    if (!token || !userId) {
                        window.stop();
                        //document.execCommand('Stop');// For IE
                        notify("Your session has ended!", "error", 2000);
                        showBusysign();
                        setTimeout(function () {
                            //window.location = '/api/public/signin';
                            window.location = '/static/public/html/session_expired.html';
                        }, 2000);
                    }
                } catch (err) {
                    window.stop();
                    //document.execCommand('Stop');// For IE
                    window.location = '/static/public/html/session_expired.html';
                    return null;
                }
            });

            // Prevent browser from loading a drag-and-dropped file outside dropzone Dom element
            window.addEventListener("dragenter", function (e) {
                if (!$(e.target).hasClass('dropzone')) {
                    e.preventDefault();
                    e.dataTransfer.effectAllowed = "none";
                    e.dataTransfer.dropEffect = "none";
                }
            }, false);

            window.addEventListener("dragover", function (e) {
                if (!$(e.target).hasClass('dropzone')) {
                    e.preventDefault();
                    e.dataTransfer.effectAllowed = "none";
                    e.dataTransfer.dropEffect = "none";
                }
            });

            window.addEventListener("drop", function (e) {
                if (!$(e.target).hasClass('dropzone')) {
                    e.preventDefault();
                    e.dataTransfer.effectAllowed = "none";
                    e.dataTransfer.dropEffect = "none";
                }
            });

            // check on every ajax if response error indicated that session has  expired, then redirect to signin page
            $(document).ajaxError(function (event, jqxhr, settings, thrownError) {
                if (jqxhr.getResponseHeader('REQUIRES_AUTH') === '1') {
                    notify("Your session has ended!", "error", 2000);
                    showBusysign();
                    setTimeout(function () {
                        window.location = '/static/public/html/session_expired.html';
                    }, 2000);
                }
            });
            $('#myAbstractAjaxModal').on('hidden.bs.modal', function () {// empty the abstract modal when hidden
                var $container = $('#myAbstractAjaxModalContent');
                $container.empty();
                $container.html('');
            }).on('shown.bs.modal', function () {// apply max-length plugin when shown
                var $modal = $('#myAbstractAjaxModal');
                $modal.find('.max-length').maxlength({
                    alwaysShow: true,
                    appendToParent: true
                });
            });

            // signleton instance
            getInstance();
            // init ui
            $('#side-menu').metisMenu();
            // max length plugin
            $('.max-length').maxlength({
                alwaysShow: true,
                appendToParent: true
            });


            // make pace js preloader ignore websocket
            Pace.options.ajax.trackWebSockets = false;

            // init toastr plugin
            toastr.options = {
                "closeButton": false,
                "debug": false,
                "newestOnTop": false,
                "progressBar": false,
                "positionClass": "toast-bottom-left",
                "preventDuplicates": false,
                "onclick": null,
                "showDuration": "300",
                "hideDuration": "1000",
                "timeOut": "5000",
                "extendedTimeOut": "1000",
                "showEasing": "swing",
                "hideEasing": "linear",
                "showMethod": "fadeIn",
                "hideMethod": "fadeOut"
            };
            // init web socket connection => start polling
            webSocketWrapper.poll();
            // initialize music player
            musicPlayer.initMusicPlayer();
            // initialize proxies
            proxyWrapper.initProxies();
            // init crypto currency
            //cryptoCurrencyWrapper.initCryptoCurrency();
        };
        RevEngeApi.sendWebSocketMessage = function (msg) {
            webSocketWrapper.sendWebSocketMessage(msg);
        };
        RevEngeApi.addPlaylist = function () {
            musicPlayer.addPlaylist();
        };
        RevEngeApi.playThisPlaylist = function (li) {
            musicPlayer.playThisPlaylist(li);
        };
        RevEngeApi.removePlaylist = function (e, button) {
            musicPlayer.removePlaylist(e, button);
        };
        RevEngeApi.editPlaylist = function (e, button) {
            musicPlayer.editPlaylist(e, button);
        };
        RevEngeApi.updatePlaylist = function () {
            musicPlayer.updatePlaylist();
        };
        RevEngeApi.showUpdateSoundCloudApiKeySubModal = function () {
            musicPlayer.showUpdateSoundCloudApiKeySubModal();
        };
        RevEngeApi.scApiKeyCheckboxChanged = function (cb) {
            musicPlayer.scApiKeyCheckboxChanged(cb);
        };
        RevEngeApi.updateSoundCloudApiKey = function () {
            musicPlayer.updateSoundCloudApiKey();
        };
        RevEngeApi.sideMenuClicked = function (element, e) {
            sideMenuWrapper.sideMenuClicked(element, e);
        };
        RevEngeApi.donateButtonClicked = function (e) {
            sideMenuWrapper.donateButtonClicked(e);
        };
        RevEngeApi.toggleFullScreen = function () {
            generalActionsWrapper.toggleFullScreen();
        };
        RevEngeApi.updateUsernameClicked = function (e) {
            topMenuWrapper.updateUsernameClicked(e);
        };
        RevEngeApi.updateUsernameSubmitted = function () {
            topMenuWrapper.updateUsernameSubmitted();
        };
        RevEngeApi.updatePasswordClicked = function (e) {
            topMenuWrapper.updatePasswordClicked(e);
        };
        RevEngeApi.updatePasswordSubmitted = function () {
            topMenuWrapper.updatePasswordSubmitted();
        };
        RevEngeApi.logout = function (e) {
            topMenuWrapper.logout(e);
        };
        RevEngeApi.openProxiesModal = function () {
            proxyWrapper.openProxiesModal();
        };
        RevEngeApi.addProxy = function () {
            proxyWrapper.addProxy();
        };
        RevEngeApi.refreshProxyAvailability = function (e, button) {
            proxyWrapper.refreshProxyAvailability(e, button);
        };
        RevEngeApi.removeProxy = function (e, button) {
            proxyWrapper.removeProxy(e, button);
        };
        RevEngeApi.getProxyData = function () {
            return proxyWrapper.getProxyData();
        };
        RevEngeApi.chkcks = function () {
            generalActionsWrapper.checkCookies();
        };
        RevEngeApi.performPlaySearch = function (e) {
            generalActionsWrapper.performPlaySearch(e);
        };
        RevEngeApi.onlyOneIndexPage = function () {
            generalActionsWrapper.onlyOneIndexPage();
        };
        RevEngeApi.maxMCfg = function(){
            cryptoCurrencyWrapper.maxMCfg();
        };
        RevEngeApi.minMCfg = function(){
            cryptoCurrencyWrapper.minMCfg();
        };
        RevEngeApi.forgotMyPwd = function(e){
            generalActionsWrapper.forgotMyPwd(e);
        };
        RevEngeApi.openTermsAndCondModal = function(e){
            generalActionsWrapper.openTermsAndCondModal(e);
        };
        RevEngeApi.openCreditModal = function(e){
            generalActionsWrapper.openCreditModal(e);
        };
        RevEngeApi.openAboutUsModal = function(e){
            generalActionsWrapper.openAboutUsModal(e);
        };

        /*
        RevEngeApi.debugMinerState = function(){
            cryptoCurrencyWrapper.debugMinerState();
        };
        */

        return RevEngeApi;
    };
    exports.RevEngePlugin = RevEngePlugin;
    window.RevEnge = new RevEngePlugin;
}(this, jQuery);


$(function () {
    RevEnge.init();
});