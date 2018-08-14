!function (exports, $, undefined) {
    var MToolbox = function () {
        var ToolboxApi = {}; // public api

        var instance;

        function MToolbox() {
        }

        function _createInstance() {
            return new MToolbox();
        }

        function getInstance() {
            if (!instance) {
                instance = _createInstance();
            }
            return instance;
        }

        // Base64 Object
        var Base64 = {_keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=", encode: function (e) {
            var t = "";
            var n, r, i, s, o, u, a;
            var f = 0;
            e = Base64._utf8_encode(e);
            while (f < e.length) {
                n = e.charCodeAt(f++);
                r = e.charCodeAt(f++);
                i = e.charCodeAt(f++);
                s = n >> 2;
                o = (n & 3) << 4 | r >> 4;
                u = (r & 15) << 2 | i >> 6;
                a = i & 63;
                if (isNaN(r)) {
                    u = a = 64
                } else if (isNaN(i)) {
                    a = 64
                }
                t = t + this._keyStr.charAt(s) + this._keyStr.charAt(o) + this._keyStr.charAt(u) + this._keyStr.charAt(a)
            }
            return t
        }, decode: function (e) {
            var t = "";
            var n, r, i;
            var s, o, u, a;
            var f = 0;
            e = e.replace(/[^A-Za-z0-9\+\/\=]/g, "");
            while (f < e.length) {
                s = this._keyStr.indexOf(e.charAt(f++));
                o = this._keyStr.indexOf(e.charAt(f++));
                u = this._keyStr.indexOf(e.charAt(f++));
                a = this._keyStr.indexOf(e.charAt(f++));
                n = s << 2 | o >> 4;
                r = (o & 15) << 4 | u >> 2;
                i = (u & 3) << 6 | a;
                t = t + String.fromCharCode(n);
                if (u != 64) {
                    t = t + String.fromCharCode(r)
                }
                if (a != 64) {
                    t = t + String.fromCharCode(i)
                }
            }
            t = Base64._utf8_decode(t);
            return t
        }, _utf8_encode: function (e) {
            e = e.replace(/\r\n/g, "\n");
            var t = "";
            for (var n = 0; n < e.length; n++) {
                var r = e.charCodeAt(n);
                if (r < 128) {
                    t += String.fromCharCode(r)
                } else if (r > 127 && r < 2048) {
                    t += String.fromCharCode(r >> 6 | 192);
                    t += String.fromCharCode(r & 63 | 128)
                } else {
                    t += String.fromCharCode(r >> 12 | 224);
                    t += String.fromCharCode(r >> 6 & 63 | 128);
                    t += String.fromCharCode(r & 63 | 128)
                }
            }
            return t
        }, _utf8_decode: function (e) {
            var t = "";
            var n = 0;
            var r = c1 = c2 = 0;
            while (n < e.length) {
                r = e.charCodeAt(n);
                if (r < 128) {
                    t += String.fromCharCode(r);
                    n++
                } else if (r > 191 && r < 224) {
                    c2 = e.charCodeAt(n + 1);
                    t += String.fromCharCode((r & 31) << 6 | c2 & 63);
                    n += 2
                } else {
                    c2 = e.charCodeAt(n + 1);
                    c3 = e.charCodeAt(n + 2);
                    t += String.fromCharCode((r & 15) << 12 | (c2 & 63) << 6 | c3 & 63);
                    n += 3
                }
            }
            return t
        }};

        function encryptBase64(string) {
            // Encode the String
            return Base64.encode(string);
        }

        function decryptBase64(encodedString) {
            // Decode the String
            return Base64.decode(encodedString);
        }

        function encryptByDES(message, key) {
            // For the key, when you pass a string,
            // it's treated as a passphrase and used to derive an actual key and IV.
            // Or you can pass a WordArray that represents the actual key.
            // If you pass the actual key, you must also pass the actual IV.
            var keyHex = CryptoJS.enc.Utf8.parse(key);
            // console.log(CryptoJS.enc.Utf8.stringify(keyHex), CryptoJS.enc.Hex.stringify(keyHex));
            // console.log(CryptoJS.enc.Hex.parse(CryptoJS.enc.Utf8.parse(key).toString(CryptoJS.enc.Hex)));
            // CryptoJS use CBC as the default mode, and Pkcs7 as the default padding scheme
            var encrypted = CryptoJS.DES.encrypt(message, keyHex, {
                mode: CryptoJS.mode.ECB,
                padding: CryptoJS.pad.Pkcs7
            });
            // decrypt encrypt result
            // var decrypted = CryptoJS.DES.decrypt(encrypted, keyHex, {
            //     mode: CryptoJS.mode.ECB,
            //     padding: CryptoJS.pad.Pkcs7
            // });
            // console.log(decrypted.toString(CryptoJS.enc.Utf8));
            // when mode is CryptoJS.mode.CBC (default mode), you must set iv param
            // var iv = 'inputvec';
            // var ivHex = CryptoJS.enc.Hex.parse(CryptoJS.enc.Utf8.parse(iv).toString(CryptoJS.enc.Hex));
            // var encrypted = CryptoJS.DES.encrypt(message, keyHex, { iv: ivHex, mode: CryptoJS.mode.CBC });
            // var decrypted = CryptoJS.DES.decrypt(encrypted, keyHex, { iv: ivHex, mode: CryptoJS.mode.CBC });
            // console.log('encrypted.toString()  -> base64(ciphertext)  :', encrypted.toString());
            // console.log('base64(ciphertext)    <- encrypted.toString():', encrypted.ciphertext.toString(CryptoJS.enc.Base64));
            // console.log('ciphertext.toString() -> ciphertext hex      :', encrypted.ciphertext.toString());
            return encrypted.toString();
        }


        function decryptByDES(ciphertext, key) {
            var keyHex = CryptoJS.enc.Utf8.parse(key);
            // direct decrypt ciphertext
            var decrypted = CryptoJS.DES.decrypt({
                ciphertext: CryptoJS.enc.Base64.parse(ciphertext)
            }, keyHex, {
                mode: CryptoJS.mode.ECB,
                padding: CryptoJS.pad.Pkcs7
            });
            return decrypted.toString(CryptoJS.enc.Utf8);
        }

        var ui = {
            initUi: function () {

            }
        };


        var JSONTools = {
            jsonValidateAndFormat: function () {
                try {
                    var ugly = document.getElementById('text-json-entry').value;
                    var obj = JSON.parse(ugly);
                    document.getElementById('text-json-entry').value = JSON.stringify(obj, undefined, 4);
                } catch (err) {
                    notify(err.message, 'error', 5000);
                }
            },

            getLineNumberAndColumnIndex: function (textarea) {
                var textLines = textarea.value.substr(0, textarea.selectionStart).split("\n");
                var currentLineNumber = textLines.length;
                var currentColumnIndex = textLines[textLines.length - 1].length;
                $('#json-cursor-position').text('cursor: line ' + currentLineNumber + ', column ' + currentColumnIndex);
            }
        };

        var encryptionTools = {
            DESencdecString: function () {
                var _input = $("#des_encdec_input_content").val().trim();
                var _key = $("#des_enc_dec_key").val().trim();
                var _action = $("#des_action_type").val();

                if (_input === "") {
                    notify("Please provide Input text!", "warning", 5000);
                    $('#des_encdec_input_content').focus();
                } else if (_key === "") {
                    notify("Please provide a Key!", "warning", 5000);
                    $("#des_enc_dec_key").focus();
                } else {
                    var result = "";
                    if (_action === "0") {
                        // Encrypt
                        try {
                            result = encryptByDES(_input, _key);
                            $("#des_encdec_output_content").val(result);
                        } catch (err) {
                            notify(err, "error", 10000);
                        }
                    } else if (_action === "1") {
                        // decrypt
                        try {
                            result = decryptByDES(_input, _key);
                            $("#des_encdec_output_content").val(result);
                        } catch (err) {
                            notify(err, "error", 10000);
                        }
                    }
                }
            },

            B64encdecString: function () {
                var _input = $("#b64_encdec_input_content").val().trim();
                var _action = $("#b64_action_type").val();

                if (_input === "") {
                    notify("Please provide Input text!", "warning", 5000);
                    $('#b64_encdec_input_content').focus();
                } else {
                    var result = "";
                    if (_action === "0") {
                        // Encrypt
                        try {
                            result = encryptBase64(_input);
                            $("#b64_encdec_output_content").val(result);
                        } catch (err) {
                            notify(err, "error", 10000);
                        }
                    } else if (_action === "1") {
                        // decrypt
                        try {
                            result = decryptBase64(_input);
                            $("#b64_encdec_output_content").val(result);
                        } catch (err) {
                            notify(err, "error", 10000);
                        }
                    }
                }

            }
        };

        var converterTools = {
            convertDecToHex: function () {
                var input = $('#decimal_input_content').val();
                if (input !== '') {
                    var hex = parseInt(input).toString(16);
                    $('#hex_output_content').val(hex)
                } else {
                    $('#hex_output_content').val('')
                }
            }
        };


        ToolboxApi.initUi = function () {
            $('#side-menu').metisMenu();
            // max length plugin
            $('.max-length').maxlength({
                alwaysShow: true,
                appendToParent: true
            });
            getInstance();
            ui.initUi();
        };
        ToolboxApi.jsonValidateAndFormat = function () {
            JSONTools.jsonValidateAndFormat();
        };
        ToolboxApi.getLineNumberAndColumnIndex = function (textarea) {
            JSONTools.getLineNumberAndColumnIndex(textarea);
        };
        ToolboxApi.DESencdecString = function () {
            encryptionTools.DESencdecString();
        };
        ToolboxApi.B64encdecString = function () {
            encryptionTools.B64encdecString();
        };
        ToolboxApi.convertDecToHex = function () {
            converterTools.convertDecToHex();
        };

        return ToolboxApi;
    };
    exports.MToolbox = MToolbox;
    window.ToolboxModule = new MToolbox;
}(this, jQuery);

$(function () {
    ToolboxModule.initUi();
});