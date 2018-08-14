// copy html to clipboard
function html2clipboard(html, el) {
    var tmpEl;

    if (typeof el !== "undefined") {
        // you may want some specific styling for your content - then provide a custom
        // DOM node with classes, inline styles or whatever you want
        tmpEl = el;
    } else {
        // else we'll just create one
        tmpEl = document.createElement("div");

        // since we remove the element immediately we'd actually not have to style it - but IE 11 prompts us to confirm the clipboard interaction
        // and until you click the confirm button, the element would show. so: still extra stuff for IE, as usual.
        tmpEl.style.opacity = 0;
        tmpEl.style.position = "absolute";
        tmpEl.style.pointerEvents = "none";
        tmpEl.style.zIndex = -1;
        tmpEl.style.whiteSpace = "pre-wrap";
    }

    // fill it with your HTML
    tmpEl.innerHTML = html;

    // append the temporary node to the DOM
    document.body.appendChild(tmpEl);

    // select the newly added node
    var range = document.createRange();
    range.selectNode(tmpEl);
    window.getSelection().addRange(range);

    try {
        // copy
        document.execCommand("copy");
    } catch (err) {
        console.log('Oops, unable to copy' + err);
        notify('unable to copy to clipboard', 'error', '3000');
    }


    // and remove the element immediately
    document.body.removeChild(tmpEl);
}

function validateIPaddress(ipaddress) {
    if (/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(ipaddress)) {
        return (true);
    }
    return (false);
}

function validatePort(port) {
    var num = +port;
    return num >= 1 && num <= 65535 && port === num.toString();
}

function escapeRegExp(str) {
    return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}

function replaceAll(str, find, replace) {
    return str.replace(new RegExp(escapeRegExp(find), 'g'), replace);
}


function remoteInstruction(data, timeout, beforeSendCallback, successCallback, errorCallback, completeCallback) {
    var url = location.protocol + '//' + location.host + '/api/protected/remoteinstruction';

    if (url.startsWith("file")) {
        notify('Cannot reach the server!', 'error', 10000);
        return;
    }

    $.ajax({
        type: 'POST',
        url: url,
        data: data,
        dataType: 'text',
        async: true,
        timeout: timeout, // sets timeout to 3 seconds

        beforeSend: function () {
            if (beforeSendCallback)
                beforeSendCallback();
        },
        success: function (data, status, xhr) {
            if (successCallback)
                successCallback(data);

        },
        error: function (xhr) {
            var msg;
            if (xhr.responseText === "undefined" || !xhr.responseText) {
                msg = xhr.statusText;
            } else {
                msg = xhr.statusText + ": " + xhr.responseText;
            }
            notify(msg, "error", 5000);
            if (errorCallback) {
                errorCallback(msg);
            }
        },
        complete: function () {
            if (completeCallback)
                completeCallback();
        }
    });
}


// Returns a function, that, as long as it continues to be invoked, will not
// be triggered. The function will be called after it stops being called for
// N milliseconds. If `immediate` is passed, trigger the function on the
// leading edge, instead of the trailing.
function debounce(func, wait, immediate) {
    var timeout;
    return function () {
        var context = this, args = arguments;
        var later = function () {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        var callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}

// JQuery light Notifications function
// notify.dconnell.co.uk/
function notify(message, type, timeout) {
    // default values
    message = typeof message !== 'undefined' ? message : 'Hello!';
    type = typeof type !== 'undefined' ? type : 'success';
    timeout = typeof timeout !== 'undefined' ? timeout : 3000;

    // append markup if it doesn't already exist
    if ($('#notification').length < 1) {
        markup = '<div id="notification" class="information"><span>Hello!</span><a class="close" href="#">x</a></div>';
        $('body').append(markup);
    } else {
        $('#notification').remove();
        markup = '<div id="notification" class="information"><span>Hello!</span><a class="close" href="#">x</a></div>';
        $('body').append(markup);
    }

    // elements
    $notification = $('#notification');
    $notificationSpan = $('#notification span');
    $notificationClose = $('#notification a.close');

    // set the message
    $notificationSpan.text(message);

    // setup click event
    $notificationClose.click(function (e) {
        e.preventDefault();
        //$notification.css('top', '-100px');
        // TODO Animate the removal
        $notification.remove();
    });

    // for ie6, scroll to the top first
    if ($.browser.msie && $.browser.version < 7) {
        $('html').scrollTop(0);
    }

    // hide old notification, then show the new notification
    $notification.css('top', '-100px').stop().removeClass().addClass(type).animate({
        top: 0
    }, 500, function () {
        $notification.delay(timeout).animate({
            top: '-100px'
        }, 500, function () {
            $notification.remove();
        });
    });
}

// A  quick workaround to use $.browser method as it has been been removed from jQuery since v 1.9
$.browser = {};
(function () {
    $.browser.msie = false;
    $.browser.version = 0;
    if (navigator.userAgent.match(/MSIE ([0-9]+)\./)) {
        $.browser.msie = true;
        $.browser.version = RegExp.$1;
    }
})();
