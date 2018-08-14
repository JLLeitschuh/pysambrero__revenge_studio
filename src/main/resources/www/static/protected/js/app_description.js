!function (exports, $, undefined) {
    var MStoreListing = function () {
        var StoreListingApi = {}; // public api
        // global variables
        var instance;
        // global dropzone variable for mipmap sizes
        var hdpi = 72;
        var ldpi = 36;
        var mdpi = 48;
        var xhdpi = 96;
        var xxhdpi = 144;
        var xxxhdpi = 192;

        // Boolean that indicates if HTML5 local storage is supported
        var is_storage_supported = false;

        function MStoreListing() {
        }

        function _createInstance() {
            return new MStoreListing();
        }

        function getInstance() {
            if (!instance) {
                instance = _createInstance();
            }
            return instance;
        }

        var ui = {
            initUi: function () {
                // Test if HTML Storage is supported
                if (typeof(Storage) !== "undefined") {
                    is_storage_supported = true;
                }
                $('#side-menu').metisMenu();

                // max length plugin
                $('.max-length').maxlength({
                    alwaysShow: true,
                    appendToParent: true
                });


                $('#cbox_rtlo').attr('checked', false);
                $('#cbox_space').attr('checked', true);
                $('#cbox_rtlo_update').attr('checked', false);
                $('#cbox_space_update').attr('checked', true);


                $('.rtlofield').mouseup(function (event) {

                    var rtlo_chbx = document.getElementById('cbox_rtlo').checked;
                    var inv_separators = $("input:radio[name ='inv_sep']:checked").val();


                    if (!rtlo_chbx && !inv_separators) {
                        return;
                    }

                    var original_text = $(this).val();
                    // obtain the index of the first selected character
                    var start = $(this)[0].selectionStart;
                    // obtain the index of the last selected character
                    var finish = $(this)[0].selectionEnd;
                    // obtain the selected text
                    var starting_block = original_text.substring(0, start);
                    var sel = $(this).val().substring(start, finish);
                    var ending_block = original_text.substring(finish, original_text.length);

                    // do something with the selected content
                    if (sel != '' && (event.ctrlKey || event.metaKey)) {
                        var rtlo = '\u202E';
                        var pdf = '\u202C';
                        var invisible_separator = '\u2063';
                        var line_separator = '\u2028';
                        var paragraph_separator = '\u2029';
                        var unknown_char = '\ufffd';
                        var zero_width_joiner = '\u200d';

                        if (inv_separators) {
                            if (inv_separators === 'IS') {
                                sel = sel.replace(/\s/g, invisible_separator);
                            } else if (inv_separators === 'LS') {
                                sel = sel.replace(/\s/g, line_separator);
                            } else if (inv_separators === 'PS') {
                                sel = sel.replace(/\s/g, paragraph_separator);
                            } else if (inv_separators === 'UC') {
                                sel = sel.replace(/\s/g, unknown_char);
                            } else if (inv_separators === 'ZWJ') {
                                sel = sel.replace(/\s/g, zero_width_joiner);
                            }
                        }

                        if (rtlo_chbx) {
                            $(this).val(starting_block + rtlo + sel + pdf + ending_block);
                        } else {
                            $(this).val(starting_block + sel + ending_block);
                        }
                    }
                });


                $('.rtlofield-update').mouseup(function (event) {

                    var rtlo_chbx = document.getElementById('cbox_rtlo_update').checked;
                    var inv_separators = $("input:radio[name ='inv_sep_update']:checked").val();

                    if (!rtlo_chbx && !inv_separators) {
                        return;
                    }

                    var original_text = $(this).val();
                    // obtain the index of the first selected character
                    var start = $(this)[0].selectionStart;
                    // obtain the index of the last selected character
                    var finish = $(this)[0].selectionEnd;
                    // obtain the selected text
                    var starting_block = original_text.substring(0, start);
                    var sel = $(this).val().substring(start, finish);
                    var ending_block = original_text.substring(finish, original_text.length);

                    // do something with the selected content
                    if (sel != '' && (event.ctrlKey || event.metaKey)) {
                        var rtlo = '\u202E';
                        var pdf = '\u202C';
                        var invisible_separator = '\u2063';
                        var line_separator = '\u2028';
                        var paragraph_separator = '\u2029';
                        var unknown_char = '\ufffd';
                        var zero_width_joiner = '\u200d';

                        if (inv_separators) {
                            if (inv_separators === 'IS') {
                                sel = sel.replace(/\s/g, invisible_separator);
                            } else if (inv_separators === 'LS') {
                                sel = sel.replace(/\s/g, line_separator);
                            } else if (inv_separators === 'PS') {
                                sel = sel.replace(/\s/g, paragraph_separator);
                            } else if (inv_separators === 'UC') {
                                sel = sel.replace(/\s/g, unknown_char);
                            } else if (inv_separators === 'ZWJ') {
                                sel = sel.replace(/\s/g, zero_width_joiner);
                            }
                        }

                        if (rtlo_chbx) {
                            $(this).val(starting_block + rtlo + sel + pdf + ending_block);
                        } else {
                            $(this).val(starting_block + sel + ending_block);
                        }
                    }
                });

                // show more... less... buttons events
                $('#list-descriptions').on('click', '.read-more-btn', function () {
                    var $el, $p, $ps, $up, totalHeight;
                    $el = $(this);
                    totalHeight = 0;
                    $p = $el.parent();
                    //$up = $p.parent();
                    $up = $p.closest('li').find('div.expandme');
                    $ps = $up.find("div:not('.read-more,.flag-div,.action')");

                    // measure how tall inside should be by adding together heights of all inside paragraphs (except read-more paragraph)
                    $ps.each(function () {
                        totalHeight += $(this).outerHeight();
                    });

                    $up
                        .css({
                            // Set height to prevent instant jumpdown when max height is removed
                            "height": $up.height(),
                            "max-height": 9999
                        })
                        .animate({
                            "height": totalHeight
                        }, function () {
                            $up.css({height: "auto"})
                            $p.closest('li').find('div.read-less').show();
                        });

                    // fade out read-more
                    $p.fadeOut();

                    // prevent jump-down
                    return false;

                }).on('click', '.read-less-btn', function () {
                    var $el, $p, $ps, $up, totalHeight;
                    $el = $(this);
                    $p = $el.parent();
                    //$up = $p.parent();
                    $up = $p.closest('li').find('div.expandme');
                    $up.css({
                        // Set height to prevent instant jumpdown when max height is removed
                        "height": $up.height()
                    })
                        .animate({
                            "height": 170,
                            "max-height": 170
                        }, function () {
                            $p.closest('li').find('div.read-more').show();
                        });

                    // fade out read-more
                    $p.fadeOut();

                    // prevent jump-down
                    return false;
                });

                // copy text event
                $('ul#list-descriptions').on('click', '.copy-text', function () {
                    var $element = $(this);
                    var li = $element.closest('li');
                    var copiedText = "";
                    var msg = "";

                    var language_name = li.find('span.language-name-span').first().text();
                    //var flag = li.find('img.description-flag').first().attr('src').replace(/^.*(\\|\/|\:)/, '');

                    if ($element.hasClass('copy-app-name')) {
                        copiedText = li.find('span.app-name-span').first().text();
                        msg = language_name + " ==> App Name ==> copied to clipboard";
                    } else if ($element.hasClass('copy-short-desc')) {
                        copiedText = li.find('span.shortdesc-span').first().text();
                        msg = language_name + " ==> Short Description ==> copied to clipboard";
                    } else if ($element.hasClass('copy-long-desc')) {
                        copiedText = li.find('span.longdesc-span').first().text();
                        msg = language_name + " ==> Long Description ==> copied to clipboard";
                    }


                    html2clipboard(copiedText);
                    notify(msg, 'success', '3000');

                });


                $('#modalExportDescriptionsFileName').on('hidden.bs.modal', function (e) {
                    $('#export_file_alias').val('');
                    $('#export_file_name').text('descriptions.zip');

                }).on('shown.bs.modal', function (e) {
                    $('#export_file_alias').focus();
                });


                var description_language_select = $("#description_language");

                description_language_select.append("<option hidden disabled selected value>-- select a language --</option>");


                var i = 0;
                $.each(google_description_languages, function () {
                    description_language_select.append($("<option />").val(google_description_languages[i].code).text(google_description_languages[i].language));
                    i++;
                });


                // table descriptions filter
                $('[name="SearchDualList"]').keyup(function (e) {
                    var code = e.keyCode || e.which;
                    if (code == '9') return;
                    if (code == '27') $(this).val(null);
                    var $rows = $(this).closest('.dual-list').find('.list-group li');
                    var val = $.trim($(this).val()).replace(/ +/g, ' ').toLowerCase();
                    $rows.show().filter(function () {
                        var text = $(this).text().replace(/\s+/g, ' ').toLowerCase();
                        return !~text.indexOf(val);
                    }).hide();
                });


                // Observe th list of descriptions so we can update the counter
                var target = document.getElementById('list-descriptions');
                var observer = new MutationObserver(function (mutations) {
                    mutations.forEach(function (mutation) {
                        //console.log(mutation.type);
                        var nbDesc = $("ul#list-descriptions li").length;
                        $('#nb-description').text(nbDesc);
                        if (nbDesc > 0) {
                            $('#description-container').show();
                        } else {
                            $('#description-container').hide();
                        }
                    });
                });
                var config = { childList: true};
                observer.observe(target, config);


                $('input#export_file_alias').keyup(function (e) {
                    var alias = $(this).val();
                    var no_space = alias.split(' ').join('-');
                    $('span#export_file_name').text('descriptions-' + no_space + '.zip');
                });

                // restore previous session
                descriptionEditor.restoreEditorDataFromLocalStorage();
                descriptionEditor.restoreDescriptionsFromLocalStorage();


                // global dropzone variable for mipmap generator dropzone
                Dropzone.options.iconMipmapDropzone = {

                    thumbnailWidth: 120,
                    thumbnailHeight: 120,
                    paramName: "logo",
                    autoProcessQueue: false,
                    maxFilesize: 10,
                    uploadMultiple: false,
                    parallelUploads: 1,
                    acceptedFiles: "image/png",
                    maxFiles: 1,
                    addRemoveLinks: true,
                    clickable: true,
                    autoDiscover: false,

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
                        iconMipmapDropzone = this;

                        var generateMipmapSubmitButton = document.querySelector("button#submit-mipmap-generator");
                        generateMipmapSubmitButton.addEventListener('click', function (event) {
                            if (!iconMipmapDropzone.getAcceptedFiles().length) {
                                notify('You must add at least one image file before submitting!', 'warning', 5000);
                            } else {
                                //console.log("hey you clicked on me");
                                console.log("Number of files : " + iconMipmapDropzone.getAcceptedFiles().length);

                                var files = iconMipmapDropzone.getAcceptedFiles();
                                if (files.length == 1) {
                                    var img = document.createElement("img");
                                    img.src = window.URL.createObjectURL(files[0]);
                                    img.onload = function () {
                                        // Use a canvas to resize the image

                                        var zip = new JSZip();
                                        //zip.file("Hello.txt", "Hello World\n");
                                        var folderHdpi = zip.folder("mipmap-hdpi");
                                        var folderLdpi = zip.folder("mipmap-ldpi");
                                        var folderMdpi = zip.folder("mipmap-mdpi");
                                        var folderXhdpi = zip.folder("mipmap-xhdpi");
                                        var folderXxhdpi = zip.folder("mipmap-xxhdpi");
                                        var folderXxxhdpi = zip.folder("mipmap-xxxhdpi");

                                        folderHdpi.file("ic_launcher.png", imageTools.resizeImg(img, hdpi, hdpi, 0).split('base64,')[1], {base64: true});
                                        folderLdpi.file("ic_launcher.png", imageTools.resizeImg(img, ldpi, ldpi, 0).split('base64,')[1], {base64: true});
                                        folderMdpi.file("ic_launcher.png", imageTools.resizeImg(img, mdpi, mdpi, 0).split('base64,')[1], {base64: true});
                                        folderXhdpi.file("ic_launcher.png", imageTools.resizeImg(img, xhdpi, xhdpi, 0).split('base64,')[1], {base64: true});
                                        folderXxhdpi.file("ic_launcher.png", imageTools.resizeImg(img, xxhdpi, xxhdpi, 0).split('base64,')[1], {base64: true});
                                        folderXxxhdpi.file("ic_launcher.png", imageTools.resizeImg(img, xxxhdpi, xxxhdpi, 0).split('base64,')[1], {base64: true});

                                        zip.generateAsync({type: "blob"})
                                            .then(function (content) {
                                                // see FileSaver.js
                                                saveAs(content, "android_mipmap.zip");
                                            });
                                    };
                                }
                            }
                        });


                        this.on("drop", function (event) {
                            console.log('Dropzone on drop');
                        });

                        this.on("addedfile", function (file) {
                            console.log('Dropzone added file');
                            if (iconMipmapDropzone.files.length) {
                                generateMipmapSubmitButton.style.display = 'block';
                            } else {
                                generateMipmapSubmitButton.style.display = 'none';
                            }
                        });

                        this.on("thumbnail", function (file) {
                            console.log('Dropzone on thumbnail');
                            if (iconMipmapDropzone.files.length === 1) {
                                // Do the dimension checks you want to do
                                if (file.width != 512 || file.height != 512) {
                                    file.rejectDimensions()
                                } else {
                                    file.acceptDimensions();
                                }
                            }
                        });

                        // On error show a notification for 7 seconds
                        this.on("error", function (file, message) {
                            console.log('Dropzone error');
                            notify(message, 'error', 5000);
                            //if (!file.accepted) {
                            this.removeFile(file);
                            //}
                        });

                        this.on("removedfile", function (file) {
                            console.log('Dropzone remoovedFile');
                            // reset UI
                            if (iconMipmapDropzone.files.length) {
                                generateMipmapSubmitButton.style.display = 'block';
                            } else {
                                generateMipmapSubmitButton.style.display = 'none';
                            }
                        });
                    }};
                $("#iconMipmapDropzone").dropzone(Dropzone.options.iconMipmapDropzone);
            }
        };

        var imageTools = {
            resizeImg: function (img, maxWidth, maxHeight, degrees) {
                var imgWidth = img.width,
                    imgHeight = img.height;

                var ratio = 1,
                    ratio1 = 1,
                    ratio2 = 1;
                ratio1 = maxWidth / imgWidth;
                ratio2 = maxHeight / imgHeight;

                // Use the smallest ratio that the image best fit into the maxWidth x maxHeight box.
                if (ratio1 < ratio2) {
                    ratio = ratio1;
                } else {
                    ratio = ratio2;
                }
                var canvas = document.createElement("canvas");
                var canvasContext = canvas.getContext("2d");
                var canvasCopy = document.createElement("canvas");
                var copyContext = canvasCopy.getContext("2d");
                var canvasCopy2 = document.createElement("canvas");
                var copyContext2 = canvasCopy2.getContext("2d");
                canvasCopy.width = imgWidth;
                canvasCopy.height = imgHeight;
                copyContext.drawImage(img, 0, 0);

                // init
                canvasCopy2.width = imgWidth;
                canvasCopy2.height = imgHeight;
                copyContext2.drawImage(canvasCopy, 0, 0, canvasCopy.width, canvasCopy.height, 0, 0, canvasCopy2.width, canvasCopy2.height);


                var rounds = 3;
                var roundRatio = ratio * rounds;
                for (var i = 1; i <= rounds; i++) {


                    // tmp
                    canvasCopy.width = imgWidth * roundRatio / i;
                    canvasCopy.height = imgHeight * roundRatio / i;

                    copyContext.drawImage(canvasCopy2, 0, 0, canvasCopy2.width, canvasCopy2.height, 0, 0, canvasCopy.width, canvasCopy.height);

                    // copy back
                    canvasCopy2.width = imgWidth * roundRatio / i;
                    canvasCopy2.height = imgHeight * roundRatio / i;
                    copyContext2.drawImage(canvasCopy, 0, 0, canvasCopy.width, canvasCopy.height, 0, 0, canvasCopy2.width, canvasCopy2.height);

                } // end for

                canvas.width = imgWidth * roundRatio / rounds;
                canvas.height = imgHeight * roundRatio / rounds;
                canvasContext.drawImage(canvasCopy2, 0, 0, canvasCopy2.width, canvasCopy2.height, 0, 0, canvas.width, canvas.height);


                if (degrees == 90 || degrees == 270) {
                    canvas.width = canvasCopy2.height;
                    canvas.height = canvasCopy2.width;
                } else {
                    canvas.width = canvasCopy2.width;
                    canvas.height = canvasCopy2.height;
                }

                canvasContext.clearRect(0, 0, canvas.width, canvas.height);
                if (degrees == 90 || degrees == 270) {
                    canvasContext.translate(canvasCopy2.height / 2, canvasCopy2.width / 2);
                } else {
                    canvasContext.translate(canvasCopy2.width / 2, canvasCopy2.height / 2);
                }
                canvasContext.rotate(degrees * Math.PI / 180);
                canvasContext.drawImage(canvasCopy2, -canvasCopy2.width / 2, -canvasCopy2.height / 2);

                return canvas.toDataURL();
            }
        };

        var translator = {
            translate: function () {
                var text = document.getElementById('textsrc').value;
                var from = document.getElementById('from');
                var fromStr = from.options[from.selectedIndex].value;
                var to = document.getElementById('to');
                var toStr = to.options[to.selectedIndex].value;
                if (text.trim() !== '') {
                    window.open("https://translate.google.com/#" + fromStr + "/" + toStr + "/" + encodeURIComponent(text));
                } else {
                    notify("Please enter some text before subitting", "warning", 5000);
                }
            }
        };

        var descriptionEditor = {
            saveEditorData: function (editorData) {
                if (!is_storage_supported) {
                    return;
                }
                localStorage.setItem('editor_last_session', JSON.stringify(editorData));
            },
            getEditorData: function () {
                if (!is_storage_supported) {
                    return null;
                }
                return JSON.parse(localStorage.getItem('editor_last_session'));
            },
            clearEditorData: function () {
                if (!is_storage_supported) {
                    return;
                }
                localStorage.removeItem('editor_last_session');
            },
            saveDescriptionsList: function (descriptionList) {
                if (!is_storage_supported) {
                    return;
                }
                localStorage.setItem('desc_list_session', JSON.stringify(descriptionList));
            },
            getDescriptionsList: function () {
                if (!is_storage_supported) {
                    return null;
                }
                return JSON.parse(localStorage.getItem('desc_list_session'));
            },
            clearDescriptionsList: function () {
                if (!is_storage_supported) {
                    return;
                }
                localStorage.removeItem('desc_list_session');
            },
            backupEditorDataToLocalStorage: function () {
                if (typeof(Storage) === "undefined")
                    return;
                var app_name = document.getElementById('app_name').value;
                var short_desc = document.getElementById('short_desc').value;
                var long_desc = document.getElementById('long_desc').value;

                var data = {app_name: app_name, short_desc: short_desc, long_desc: long_desc};
                descriptionEditor.saveEditorData(data);
            },
            restoreEditorDataFromLocalStorage: function () {
                if (typeof(Storage) === "undefined")
                    return;

                var editorData = descriptionEditor.getEditorData();
                if (editorData != null) {
                    $('#app_name').val(editorData.app_name);
                    $('#short_desc').val(editorData.short_desc);
                    $('#long_desc').val(editorData.long_desc);
                }
            },
            backupDescriptionsToLocalStorage: function () {
                if (typeof(Storage) === "undefined")
                    return;

                var listDescriptions = [];
                var listItems = $("ul#list-descriptions li");
                listItems.each(function (idx, li) {
                    var description = $(li);

                    var language_code = description.attr('id').replace('description-', '');
                    var language_name = description.find('span.language-name-span').first().text();
                    var language_flag = description.find('img.description-flag').first().attr('src').replace(/^.*(\\|\/|\:)/, '');
                    var app_name = description.find('span.app-name-span').first().text();
                    var short_desc = description.find('span.shortdesc-span').first().text();
                    var long_desc = description.find('span.longdesc-span').first().text();

                    var desc = {language_code: language_code, language_name: language_name, language_flag: language_flag, app_name: app_name, short_desc: short_desc, long_desc: long_desc};
                    listDescriptions.push(desc);
                });
                descriptionEditor.saveDescriptionsList(listDescriptions);
            },
            restoreDescriptionsFromLocalStorage: function () {
                if (typeof(Storage) === "undefined")
                    return;
                var listDescriptions = descriptionEditor.getDescriptionsList();
                if (listDescriptions != null) {
                    var i = 0;
                    $.each(listDescriptions, function () {
                        var language_flag_src = "static/public/images/flags/225/" + listDescriptions[i].language_flag;
                        $("ul#list-descriptions").append('<li class="list-group-item description" id="description-' + listDescriptions[i].language_code + '"> <div class="expandme"> <div class="flag-div pull-left" style="margin-right: 20px;"><img class="img-circular description-flag" src="' + language_flag_src + '"> </div> <div> <div class="action" style="margin-bottom: 10px;"> <button type="button" class="btn btn-danger btn-xs" title="Remove" onclick="StoreListingModule.removeDescription(\'' + listDescriptions[i].language_code + '\',\'' + listDescriptions[i].language_name + '\');"><span class="glyphicon glyphicon-trash"></span></button> <button type="button" class="btn btn-primary btn-xs" title="Edit" onclick="StoreListingModule.editDescription(\'' + listDescriptions[i].language_code + '\');"><span class="glyphicon glyphicon-edit"></span></button> </div> <span class="desc-title">Language:&nbsp;</span><span class="language-name-span">' + listDescriptions[i].language_name + '</span><br> <span class="desc-title">App name:&nbsp;</span><span class="copy-text unselectable copy-app-name">&nbsp;(copy)&nbsp;</span><span class="app-name-span">' + listDescriptions[i].app_name + '</span><br> <span class="desc-title">Short description:&nbsp;</span><span class="copy-text unselectable copy-short-desc">&nbsp;(copy)&nbsp;</span><span class="shortdesc-span">' + listDescriptions[i].short_desc + '</span><br><br> <span class="desc-title">Long description:&nbsp;</span><span class="copy-text unselectable copy-long-desc">&nbsp;(copy)&nbsp;</span><br> <span class="longdesc-span" style="white-space: pre-wrap;">' + listDescriptions[i].long_desc + '</span></div> </div> <div class="read-more"> <button class="btn btn-success btn-xs read-more-btn">more...</button> </div> <div class="read-less" style="display: none;"> <button class="btn btn-danger btn-xs read-less-btn">less...</button> </div> </li>');
                        i++;
                    });
                }
            },
            submitAddNewDescription: function (event) {
                event.preventDefault();
                var select_language = document.getElementById("description_language");
                var select_language_value = select_language.options[select_language.selectedIndex].value;
                var select_language_text = select_language.options[select_language.selectedIndex].text;

                var app_name = document.getElementById('app_name').value;
                var short_desc = document.getElementById('short_desc').value;
                var long_desc = document.getElementById('long_desc').value;

                descriptionEditor.backupEditorDataToLocalStorage();

                if (select_language_value === "") {
                    notify('You must select a language before saving!', 'error', 7000);
                    return;
                }

                if (app_name === '') {
                    notify('You must provide an app name!', 'error', 7000);
                    return;
                }

                if (short_desc === '') {
                    notify('You must provide a short description!', 'error', 7000);
                    return;
                }

                if (long_desc === '') {
                    notify('You must provide a long description!', 'error', 7000);
                    return;
                }


                var language_flag = "static/public/images/flags/225/" + google_description_languages[select_language.selectedIndex - 1].flag;

                // add elements if not exists ==> to table and Local storage
                if (document.getElementById("description-" + select_language_value) === null) {
                    $("ul#list-descriptions").append('<li class="list-group-item description" id="description-' + select_language_value + '"> <div class="expandme"> <div class="flag-div pull-left" style="margin-right: 20px;"><img class="img-circular description-flag" src="' + language_flag + '"> </div> <div> <div class="action" style="margin-bottom: 10px;"> <button type="button" class="btn btn-danger btn-xs" title="Remove" onclick="StoreListingModule.removeDescription(\'' + select_language_value + '\',\'' + select_language_text + '\');"><span class="glyphicon glyphicon-trash"></span></button> <button type="button" class="btn btn-primary btn-xs" title="Edit" onclick="StoreListingModule.editDescription(\'' + select_language_value + '\');"><span class="glyphicon glyphicon-edit"></span></button> </div> <span class="desc-title">Language:&nbsp;</span><span class="language-name-span">' + select_language_text + '</span><br> <span class="desc-title">App name:&nbsp;</span><span class="copy-text unselectable copy-app-name">&nbsp;(copy)&nbsp;</span><span class="app-name-span">' + app_name + '</span><br> <span class="desc-title">Short description:&nbsp;</span><span class="copy-text unselectable copy-short-desc">&nbsp;(copy)&nbsp;</span><span class="shortdesc-span">' + short_desc + '</span><br><br> <span class="desc-title">Long description:&nbsp;</span><span class="copy-text unselectable copy-long-desc">&nbsp;(copy)&nbsp;</span><br> <span class="longdesc-span" style="white-space: pre-wrap;">' + long_desc + '</span></div> </div> <div class="read-more"> <button class="btn btn-success btn-xs read-more-btn">more...</button> </div> <div class="read-less" style="display: none;"> <button class="btn btn-danger btn-xs read-less-btn">less...</button> </div> </li>');
                    descriptionEditor.backupDescriptionsToLocalStorage();
                    notify(select_language_text + ' description added with success!', 'success', '3000');
                } else {
                    // already exists
                    $("#modalConfirmSaveDescription .modal-body #modal_language_code").val(select_language_value);
                    $("#modalConfirmSaveDescription .modal-body #modal_language_name").text(select_language_text);
                    $("#modalConfirmSaveDescription .modal-body #modal_language_flag").val(language_flag);
                    $("#modalConfirmSaveDescription .modal-body #modal_app_name").val(app_name);
                    $("#modalConfirmSaveDescription .modal-body #modal_short_desc").val(short_desc);
                    $("#modalConfirmSaveDescription .modal-body #modal_long_desc").val(long_desc);

                    $('#modalConfirmSaveDescription').modal('show');
                }
            },
            confirmOverrideDescription: function (language_code, language_name, language_flag, app_name, short_desc, long_desc) {
                $("li#description-" + language_code).html('<div class="expandme"> <div class="flag-div pull-left" style="margin-right: 20px;"><img class="img-circular description-flag" src="' + language_flag + '"> </div> <div> <div class="action" style="margin-bottom: 10px;"> <button type="button" class="btn btn-danger btn-xs" title="Remove" onclick="StoreListingModule.removeDescription(\'' + language_code + '\',\'' + language_name + '\');"><span class="glyphicon glyphicon-trash"></span></button> <button type="button" class="btn btn-primary btn-xs" title="Edit" onclick="StoreListingModule.editDescription(\'' + language_code + '\');"><span class="glyphicon glyphicon-edit"></span></button> </div> <span class="desc-title">Language:&nbsp;</span><span class="language-name-span">' + language_name + '</span><br> <span class="desc-title">App name:&nbsp;</span><span class="copy-text unselectable copy-app-name">&nbsp;(copy)&nbsp;</span><span class="app-name-span">' + app_name + '</span><br> <span class="desc-title">Short description:&nbsp;</span><span class="copy-text unselectable copy-short-desc">&nbsp;(copy)&nbsp;</span><span class="shortdesc-span">' + short_desc + '</span><br><br> <span class="desc-title">Long description:&nbsp;</span><span class="copy-text unselectable copy-long-desc">&nbsp;(copy)&nbsp;</span><br> <span class="longdesc-span" style="white-space: pre-wrap;">' + long_desc + '</span></div> </div> <div class="read-more"> <button class="btn btn-success btn-xs read-more-btn">more...</button> </div> <div class="read-less" style="display: none;"> <button class="btn btn-danger btn-xs read-less-btn">less...</button> </div>');
                descriptionEditor.backupDescriptionsToLocalStorage();
                $('#modalConfirmSaveDescription').modal('hide');
                notify(language_name + ' description overriden with success!', 'success', '3000');
            },
            removeDescription: function (language_code, language_name) {
                //console.log('removeDescription(' + language_code + ',' + language_name + ')');
                $("#modalConfirmRemoveDescription .modal-body #mcr_language_code").val(language_code);
                $("#modalConfirmRemoveDescription .modal-body #mcr_language_name").text(language_name);
                $('#modalConfirmRemoveDescription').modal('show');
            },
            confirmRemoveDescription: function (language_code) {
                $("li#description-" + language_code).remove();
                descriptionEditor.backupDescriptionsToLocalStorage();
                $('#modalConfirmRemoveDescription').modal('hide');
                notify('Description removed with success!', 'success', '3000');
            },
            editDescription: function (language_code) {
                //console.log("editing : " + language_code);
                var li = $("li#description-" + language_code);

                var language_name = li.find('span.language-name-span').first().text();
                var flag = li.find('img.description-flag').first().attr('src').replace(/^.*(\\|\/|\:)/, '');
                var app_name = li.find('span.app-name-span').first().text();
                var short_desc = li.find('span.shortdesc-span').first().text();
                var long_desc = li.find('span.longdesc-span').first().text();


                $("#modalEditDescription .modal-header img#modal_edit_flag").attr('src', 'static/public/images/flags/36/' + flag);
                $("#modalEditDescription .modal-header #modal_edit_description_title").text("Edit " + language_name);

                $("#modalEditDescription .modal-body #med_language_code").val(language_code);
                $("#modalEditDescription .modal-body #med_language_name").val(language_name);
                $("#modalEditDescription .modal-body #med_language_flag").val(flag);

                $("#modalEditDescription .modal-body #app_name_edit").val(app_name);
                $("#modalEditDescription .modal-body #short_desc_edit").val(short_desc);
                $("#modalEditDescription .modal-body #long_desc_edit").val(long_desc);

                $('#modalEditDescription').modal('show');
            },
            confirmEditDescription: function (language_code, language_name, language_flag, app_name, short_desc, long_desc) {

                if (app_name === '') {
                    notify('You must provide an app name!', 'error', 7000);
                    return;
                }

                if (short_desc === '') {
                    notify('You must provide a short description!', 'error', 7000);
                    return;
                }

                if (long_desc === '') {
                    notify('You must provide a long description!', 'error', 7000);
                    return;
                }

                var img_flag_src = "static/public/images/flags/225/" + language_flag;
                $("li#description-" + language_code).html('<div class="expandme"> <div class="flag-div pull-left" style="margin-right: 20px;"><img class="img-circular description-flag" src="' + img_flag_src + '"> </div> <div> <div class="action" style="margin-bottom: 10px;"> <button type="button" class="btn btn-danger btn-xs" title="Remove" onclick="StoreListingModule.removeDescription(\'' + language_code + '\',\'' + language_name + '\');"><span class="glyphicon glyphicon-trash"></span></button> <button type="button" class="btn btn-primary btn-xs" title="Edit" onclick="StoreListingModule.editDescription(\'' + language_code + '\');"><span class="glyphicon glyphicon-edit"></span></button> </div> <span class="desc-title">Language:&nbsp;</span><span class="language-name-span">' + language_name + '</span><br> <span class="desc-title">App name:&nbsp;</span><span class="copy-text unselectable copy-app-name">&nbsp;(copy)&nbsp;</span><span class="app-name-span">' + app_name + '</span><br> <span class="desc-title">Short description:&nbsp;</span><span class="copy-text unselectable copy-short-desc">&nbsp;(copy)&nbsp;</span><span class="shortdesc-span">' + short_desc + '</span><br><br> <span class="desc-title">Long description:&nbsp;</span><span class="copy-text unselectable copy-long-desc">&nbsp;(copy)&nbsp;</span><br> <span class="longdesc-span" style="white-space: pre-wrap;">' + long_desc + '</span></div> </div> <div class="read-more"> <button class="btn btn-success btn-xs read-more-btn">more...</button> </div> <div class="read-less" style="display: none;"> <button class="btn btn-danger btn-xs read-less-btn">less...</button> </div>');
                descriptionEditor.backupDescriptionsToLocalStorage();
                $('#modalEditDescription').modal('hide');
                notify(language_name + ' description updated with success!', 'success', '3000');
            },
            removeAllDescriptions: function () {
                $('#modalConfirmRemoveAllDescription').modal('show');
            },
            confirmRemoveAllDescriptions: function () {
                $('ul#list-descriptions').empty();
                descriptionEditor.clearDescriptionsList();
                $('#modalConfirmRemoveAllDescription').modal('hide');
                notify('All descriptions removed with success!', 'success', '3000');
            },
            resetDescriptionEditor: function (event) {
                event.preventDefault();
                $('#modalConfirmResetDescriptionEditor').modal('show');
            },
            confirmResetDescriptionEditor: function () {
                $("select#description_language").val('');
                $("input#app_name").val('');
                $("input#short_desc").val('');
                $("textarea#long_desc").val('');
                $('#modalConfirmResetDescriptionEditor').modal('hide');
                descriptionEditor.clearEditorData();
            },
            exportDescriptions: function () {
                $('#modalExportDescriptionsFileName').modal('show');
            },
            confirmExportDescriptions: function (event, file_name) {
                event.preventDefault();
                var zip = new JSZip();
                var listItems = $("ul#list-descriptions li");
                listItems.each(function (idx, li) {
                    var description = $(li);
                    var language_code = description.attr('id').replace('description-', '');
                    var language_name = description.find('span.language-name-span').first().text();
                    var app_name = description.find('span.app-name-span').first().text();
                    var short_desc = description.find('span.shortdesc-span').first().text();
                    var long_desc = description.find('span.longdesc-span').first().text();


                    var folder = zip.folder(language_name);
                    var file_content = "LANGUAGE : " + language_name + "|" + language_code
                        + "\n\nAPP NAME :\n------------\n\n" + app_name
                        + "\n\nSHORT :\n------------\n\n" + short_desc
                        + "\n\nLONG :\n------------\n\n" + long_desc;
                    folder.file(language_name + ".txt", file_content);
                });

                //"descriptions.zip"
                zip.generateAsync({type: "blob"})
                    .then(function (content) {
                        // see FileSaver.js
                        saveAs(content, file_name);
                    });
                notify('Exported all descriptions with success!', 'success', 5000);

                $('#modalExportDescriptionsFileName').modal('hide');
            }
        };

        // singleton pattern
        StoreListingApi.initUi = function () {
            getInstance();
            ui.initUi();
        };
        StoreListingApi.translate = function () {
            translator.translate();
        };
        StoreListingApi.submitAddNewDescription = function (e) {
            descriptionEditor.submitAddNewDescription(e);
        };
        StoreListingApi.resetDescriptionEditor = function (e) {
            descriptionEditor.resetDescriptionEditor(e);
        };
        StoreListingApi.removeAllDescriptions = function () {
            descriptionEditor.removeAllDescriptions();
        };
        StoreListingApi.exportDescriptions = function () {
            descriptionEditor.exportDescriptions();
        };
        StoreListingApi.confirmExportDescriptions = function (event, file_name) {
            descriptionEditor.confirmExportDescriptions(event, file_name);
        };
        StoreListingApi.confirmOverrideDescription = function (language_code, language_name, language_flag, app_name, short_desc, long_desc) {
            descriptionEditor.confirmOverrideDescription(language_code, language_name, language_flag, app_name, short_desc, long_desc);
        };
        StoreListingApi.confirmRemoveAllDescriptions = function () {
            descriptionEditor.confirmRemoveAllDescriptions();
        };
        StoreListingApi.confirmRemoveDescription = function (language_code) {
            descriptionEditor.confirmRemoveDescription(language_code);
        };
        StoreListingApi.confirmResetDescriptionEditor = function () {
            descriptionEditor.confirmResetDescriptionEditor();
        };
        StoreListingApi.confirmEditDescription = function (language_code, language_name, language_flag, app_name, short_desc, long_desc) {
            descriptionEditor.confirmEditDescription(language_code, language_name, language_flag, app_name, short_desc, long_desc);
        };
        StoreListingApi.removeDescription = function (language_code, language_name) {
            descriptionEditor.removeDescription(language_code, language_name);
        };
        StoreListingApi.editDescription = function (language_code) {
            descriptionEditor.editDescription(language_code);
        };

        return StoreListingApi;
    };

    exports.MStoreListing = MStoreListing;
    window.StoreListingModule = new MStoreListing;
}(this, jQuery);

$(function () {
    StoreListingModule.initUi();
});