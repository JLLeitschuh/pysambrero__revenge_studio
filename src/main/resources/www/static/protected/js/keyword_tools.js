!function (exports, $, undefined) {
    var MKeywordTools = function () {
        var KeywordToolsApi = {}; // public api

        var instance;

        function MKeywordTools() {
        }

        function _createInstance() {
            return new MKeywordTools();
        }

        function getInstance() {
            if (!instance) {
                instance = _createInstance();
            }
            return instance;
        }

        var ui = {
            init: function () {
                unicodeDetector.init();
                keywordDensity.init();
                googleTrends.init();
            }
        };

        var unicodeDetector = {
            init: function () {
                var UnicodeDetector = function (options) {
                    this.init(options);
                };

                UnicodeDetector.prototype.init = function (options) {
                    this.msgArea = $('#detector-textarea');
                    this.charContainer = $('#character-container');
                    this.typerMessage = '';
                    this.typerTimeout = options || 50;
                    this.mapGSMExtended = ["\n", '^', '{', '}', '\\', '[', '~', ']', '|', 'â‚¬'];
                    this.clickedOnPlaceholder = false;
                    var that = this;
                    this.msgArea.on('click', function () {
                        if (!that.clickedOnPlaceholder) {
                            that.clickedOnPlaceholder = true;
                            that.msgArea.val('').trigger('change');
                        }
                    });
                    this.typeMessage();
                };

                UnicodeDetector.prototype.typeMessage = function () {
                    var that = this;
                    var typer = setInterval(function () {
                        var c = that.typerMessage.slice(0, 1);
                        that.typerMessage = that.typerMessage.slice(1);
                        that.msgArea.val(that.msgArea.val() + c);
                        that.msgArea.focus();
                        that.msgArea.trigger('input.count');
                        if (that.typerMessage.length === 0) {
                            clearInterval(typer);
                        }
                    }, this.typerTimeout);
                };

                UnicodeDetector.prototype.renderCharContainer = function () {
                    var str = this.msgArea.val();
                    this.charContainer.html('');
                    for (var i = 0; i < str.length; i++) {
                        var ch = str.charAt(i);
                        //var isUnicode = ch.isUnicode();
                        var isUnicode = containsNonLatinCodepoints(ch);
                        var attr = {html: ch};
                        if (ch === ' ') {
                            attr.html = '&nbsp;';
                        }
                        if (ch === "\n") {
                            attr['html'] = 'â†µ';
                        }
                        if (this.mapGSMExtended.indexOf(ch) !== -1) {
                            attr['class'] = 'warning';
                        }
                        if (isUnicode) {
                            var code = '';
                            attr['class'] = 'danger';
                            if (ch === '\u202e' || ch === '\u202c' || ch === '\u2063' || ch === '\u2028' || ch === '\u2029' || ch === '\ufffd' || ch === '\u200d') {
                                attr.html = ' ';
                            }
                            if (ch === '\u202e') {
                                code = 'u202e RTLO';
                            }
                            if (ch === '\u202c') {
                                code = 'u202c PDF (pop directional formatting)';
                            }
                            if (ch === '\u200d') {
                                code = 'u200d ZWJ (zero width joiner)';
                            }
                            if (ch === '\u2063') {
                                code = 'u2063 IS (invisible separator)';
                            }
                            if (ch === '\u2028') {
                                code = 'u2028 LS (line separator)';
                            }
                            if (ch === '\u2029') {
                                code = 'u2029 PS (paragraph separator)';
                            }
                            if (ch === '\ufffd') {
                                code = 'ufffd UC (unknown character)';
                            }

                            var $brick = $('<span></span>');
                            if (attr['class']) {
                                $brick.addClass(attr['class']);
                            }

                            $brick.text(attr['html']);

                            if (code !== '')
                                $brick.attr('title', code);

                            this.charContainer.append($brick);
                        } else {
                            this.charContainer.append(ch);
                        }
                    }
                };

                String.prototype.isUnicode = function () {
                    var reg = new RegExp("[^A-Za-z0-9 \u00A0\\r\\n\\t@Â£$Â¥Ã¨Ã©Ã¹Ã¬Ã²Ã‡Ã˜Ã¸Ã…Ã¥\u0394_\u03A6\u0393\u039B\u03A9\u03A0\u03A8\u03A3\u0398\u039E\u202E\u202CÃ†Ã¦ÃŸÃ‰!\"#$%&'()*+,\\-./:;<=>?Â¡Ã„Ã–Ã‘ÃœÂ§Â¿Ã¤Ã¶Ã±Ã¼Ã ^{}\\\\\\[~\\]|\u20AC\u2013\u2014ÃµÃ§`â€™Â´â€˜â€™â€²â€³â€œâ€“â€‘âˆ’â€”ä¸€Â«Â»â€ï¼ï¼šâ€¢Â®Î¿Îº]");
                    return reg.test(this);
                };

                String.prototype.capitalize = function () {
                    return this.charAt(0).toUpperCase() + this.slice(1);
                };

                // onchange => detect unicode
                var u = new UnicodeDetector();
                u.msgArea.on('input.count', function () {
                    u.renderCharContainer();
                });
            },
            resetUnicodeDetector: function () {
                $('#detector-textarea').val('');
                $('#character-container').empty();
            }
        };

        var keywordDensity = {
            init: function () {
                // onchange => calculate keyword density
                $("#kw-textarea").keyup(function () {
                    keywordDensity.txtKeyUp();
                });
            },
            tkw: null,
            words: null,
            //var stopwords = ["،","أ","ا","اثر","اجل","احد","اخرى","اذا","اربعة","اطار","اعادة","اعلنت","اف","اكثر","اكد","الا","الاخيرة","الان","الاول","الاولى","التى","التي","الثاني","الثانية","الذاتي","الذى","الذي","الذين","السابق","الف","الماضي","المقبل","الوقت","الى","اليوم","اما","امام","امس","ان","انه","انها","او","اول","اي","ايار","ايام","ايضا","ب","باسم","بان","برس","بسبب","بشكل","بعد","بعض","بن","به","بها","بين","تم","ثلاثة","ثم","جميع","حاليا","حتى","حوالى","حول","حيث","حين","خلال","دون","ذلك","زيارة","سنة","سنوات","شخصا","صباح","صفر","ضد","ضمن","عام","عاما","عدة","عدد","عدم","عشر","عشرة","على","عليه","عليها","عن","عند","عندما","غدا","غير","ـ","ف","فان","فى","في","فيه","فيها","قال","قبل","قد","قوة","كان","كانت","كل","كلم","كما","لا","لدى","لقاء","لكن","للامم","لم","لن","له","لها","لوكالة","ما","مايو","مساء","مع","مقابل","مليار","مليون","من","منذ","منها","نحو","نفسه","نهاية","هذا","هذه","هناك","هو","هي","و","و6","واحد","واضاف","واضافت","واكد","وان","واوضح","وفي","وقال","وقالت","وقد","وقف","وكان","وكانت","ولا","ولم","ومن","وهو","وهي","يكون","يمكن","يوم"];
            //var stopwords = ["a","abord","absolument","afin","ah","ai","aie","ailleurs","ainsi","ait","allaient","allo","allons","allô","alors","anterieur","anterieure","anterieures","apres","après","as","assez","attendu","au","aucun","aucune","aujourd","aujourd'hui","aupres","auquel","aura","auraient","aurait","auront","aussi","autre","autrefois","autrement","autres","autrui","aux","auxquelles","auxquels","avaient","avais","avait","avant","avec","avoir","avons","ayant","b","bah","bas","basee","bat","beau","beaucoup","bien","bigre","boum","bravo","brrr","c","car","ce","ceci","cela","celle","celle-ci","celle-là","celles","celles-ci","celles-là","celui","celui-ci","celui-là","cent","cependant","certain","certaine","certaines","certains","certes","ces","cet","cette","ceux","ceux-ci","ceux-là","chacun","chacune","chaque","cher","chers","chez","chiche","chut","chère","chères","ci","cinq","cinquantaine","cinquante","cinquantième","cinquième","clac","clic","combien","comme","comment","comparable","comparables","compris","concernant","contre","couic","crac","d","da","dans","de","debout","dedans","dehors","deja","delà","depuis","dernier","derniere","derriere","derrière","des","desormais","desquelles","desquels","dessous","dessus","deux","deuxième","deuxièmement","devant","devers","devra","different","differentes","differents","différent","différente","différentes","différents","dire","directe","directement","dit","dite","dits","divers","diverse","diverses","dix","dix-huit","dix-neuf","dix-sept","dixième","doit","doivent","donc","dont","douze","douzième","dring","du","duquel","durant","dès","désormais","e","effet","egale","egalement","egales","eh","elle","elle-même","elles","elles-mêmes","en","encore","enfin","entre","envers","environ","es","est","et","etant","etc","etre","eu","euh","eux","eux-mêmes","exactement","excepté","extenso","exterieur","f","fais","faisaient","faisant","fait","façon","feront","fi","flac","floc","font","g","gens","h","ha","hein","hem","hep","hi","ho","holà","hop","hormis","hors","hou","houp","hue","hui","huit","huitième","hum","hurrah","hé","hélas","i","il","ils","importe","j","je","jusqu","jusque","juste","k","l","la","laisser","laquelle","las","le","lequel","les","lesquelles","lesquels","leur","leurs","longtemps","lors","lorsque","lui","lui-meme","lui-même","là","lès","m","ma","maint","maintenant","mais","malgre","malgré","maximale","me","meme","memes","merci","mes","mien","mienne","miennes","miens","mille","mince","minimale","moi","moi-meme","moi-même","moindres","moins","mon","moyennant","multiple","multiples","même","mêmes","n","na","naturel","naturelle","naturelles","ne","neanmoins","necessaire","necessairement","neuf","neuvième","ni","nombreuses","nombreux","non","nos","notamment","notre","nous","nous-mêmes","nouveau","nul","néanmoins","nôtre","nôtres","o","oh","ohé","ollé","olé","on","ont","onze","onzième","ore","ou","ouf","ouias","oust","ouste","outre","ouvert","ouverte","ouverts","o|","où","p","paf","pan","par","parce","parfois","parle","parlent","parler","parmi","parseme","partant","particulier","particulière","particulièrement","pas","passé","pendant","pense","permet","personne","peu","peut","peuvent","peux","pff","pfft","pfut","pif","pire","plein","plouf","plus","plusieurs","plutôt","possessif","possessifs","possible","possibles","pouah","pour","pourquoi","pourrais","pourrait","pouvait","prealable","precisement","premier","première","premièrement","pres","probable","probante","procedant","proche","près","psitt","pu","puis","puisque","pur","pure","q","qu","quand","quant","quant-à-soi","quanta","quarante","quatorze","quatre","quatre-vingt","quatrième","quatrièmement","que","quel","quelconque","quelle","quelles","quelqu'un","quelque","quelques","quels","qui","quiconque","quinze","quoi","quoique","r","rare","rarement","rares","relative","relativement","remarquable","rend","rendre","restant","reste","restent","restrictif","retour","revoici","revoilà","rien","s","sa","sacrebleu","sait","sans","sapristi","sauf","se","sein","seize","selon","semblable","semblaient","semble","semblent","sent","sept","septième","sera","seraient","serait","seront","ses","seul","seule","seulement","si","sien","sienne","siennes","siens","sinon","six","sixième","soi","soi-même","soit","soixante","son","sont","sous","souvent","specifique","specifiques","speculatif","stop","strictement","subtiles","suffisant","suffisante","suffit","suis","suit","suivant","suivante","suivantes","suivants","suivre","superpose","sur","surtout","t","ta","tac","tant","tardive","te","tel","telle","tellement","telles","tels","tenant","tend","tenir","tente","tes","tic","tien","tienne","tiennes","tiens","toc","toi","toi-même","ton","touchant","toujours","tous","tout","toute","toutefois","toutes","treize","trente","tres","trois","troisième","troisièmement","trop","très","tsoin","tsouin","tu","té","u","un","une","unes","uniformement","unique","uniques","uns","v","va","vais","vas","vers","via","vif","vifs","vingt","vivat","vive","vives","vlan","voici","voilà","vont","vos","votre","vous","vous-mêmes","vu","vé","vôtre","vôtres","w","x","y","z","zut","à","â","ça","ès","étaient","étais","était","étant","été","être","ô"];

            stopwords: [
                'a', 'about', 'above', 'above', 'across', 'after', 'afterwards', 'again', 'against', 'all',
                'almost', 'alone', 'along', 'already', 'also', 'although', 'always', 'am', 'among', 'amongst', 'amoungst',
                'amount', 'an', 'and', 'another', 'any', 'anyhow', 'anyone', 'anything', 'anyway', 'anywhere', 'are',
                'around', 'as', 'at', 'back', 'be', 'became', 'because', 'become', 'becomes', 'becoming', 'been',
                'before', 'beforehand', 'behind', 'being', 'below', 'beside', 'besides', 'between', 'beyond', 'bill',
                'both', 'bottom', 'but', 'by', 'call', 'can', 'cannot', 'cant', 'co', 'con', 'could', 'couldnt', 'cry',
                'de', 'describe', 'detail', 'do', 'done', 'down', 'due', 'during', 'each', 'eg', 'eight', 'either',
                'eleven', 'else', 'elsewhere', 'empty', 'enough', 'etc', 'even', 'ever', 'every', 'everyone', 'everything',
                'everywhere', 'except', 'few', 'fifteen', 'fify', 'fill', 'find', 'fire', 'first', 'five', 'former',
                'formerly', 'forty', 'found', 'four', 'from', 'front', 'full', 'further', 'get', 'give', 'go', 'had',
                'has', 'hasnt', 'have', 'he', 'hence', 'her', 'here', 'hereafter', 'hereby', 'herein', 'hereupon', 'hers',
                'herself', 'him', 'himself', 'his', 'how', 'however', 'hundred', 'ie', 'if', 'in', 'inc', 'indeed', 'interest',
                'into', 'is', 'it', 'its', 'itself', 'keep', 'last', 'latter', 'latterly', 'least', 'less', 'ltd', 'made',
                'many', 'may', 'me', 'meanwhile', 'might', 'mill', 'mine', 'more', 'moreover', 'most', 'mostly', 'move',
                'much', 'must', 'my', 'myself', 'name', 'namely', 'neither', 'never', 'nevertheless', 'next', 'nine', 'no',
                'nobody', 'none', 'noone', 'nor', 'not', 'nothing', 'now', 'nowhere', 'off', 'often', 'on', 'once',
                'one', 'only', 'onto', 'or', 'other', 'others', 'otherwise', 'our', 'ours', 'ourselves', 'out', 'over',
                'own', 'part', 'per', 'perhaps', 'please', 'put', 'rather', 're', 'same', 'see', 'seem', 'seemed', 'seeming',
                'seems', 'serious', 'several', 'she', 'should', 'show', 'side', 'since', 'sincere', 'six', 'sixty', 'so',
                'some', 'somehow', 'someone', 'something', 'sometime', 'sometimes', 'somewhere', 'still', 'such', 'system',
                'take', 'ten', 'than', 'that', 'the', 'their', 'them', 'themselves', 'then', 'thence', 'there', 'thereafter',
                'thereby', 'therefore', 'therein', 'thereupon', 'these', 'they', 'thickv', 'thin', 'third', 'this', 'those',
                'though', 'three', 'through', 'throughout', 'thru', 'thus', 'to', 'together', 'too', 'top', 'toward',
                'towards', 'twelve', 'twenty', 'two', 'un', 'under', 'until', 'up', 'upon', 'us', 'very', 'via', 'was', 'we',
                'well', 'were', 'what', 'whatever', 'when', 'whence', 'whenever', 'where', 'whereafter', 'whereas', 'whereby',
                'wherein', 'whereupon', 'wherever', 'whether', 'which', 'while', 'whither', 'who', 'whoever', 'whole', 'whom',
                'whose', 'why', 'will', 'with', 'within', 'without', 'would', 'yet', 'you', 'your', 'yours', 'yourself',
                'yourselves', 'the', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ''
            ],
            delay: null,
            txtKeyUp: function () {
                clearTimeout(keywordDensity.delay);
                keywordDensity.delay = setTimeout(keywordDensity.displayDensity, 1500);
            },
            clearHebrewPoints: function (str) {
                return str.replace(/[\u05B0-\u05C4]/gi, '');
            },
            clearUrls: function (str) {
                return str.replace(/https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/gi, ' ');
            },

            clearHtml: function (str) {
                return str.replace(/<img.*alt=(["'])([^\1]*?)\1.*>/gi, " $2 ").replace(/<a.*title=(["'])([^\1]*?)\1.*>/gi, " $2 ").replace(/<[^>]*>/g, "");
            },
            clearEntities: function (str) {
                return str.replace(/&nbsp;/gi, ' ').replace(/&amp;/gi, '&').replace(/&quot;/gi, '"').replace(/&apos;/gi, "'").replace(/&lt;/gi, '<').replace(/&gt;/gi, '>').replace(/&(copy|reg);/gi, ' ');
            },
            clearSigns: function (str) {
                return str.replace(/'s /gi, ' ').replace(/['"?!]/g, '').replace(/[,.]/g, ' ');
            },
            clearStopWords: function (str) {
                $.each(keywordDensity.stopwords, function (index, stopword) {
                    str.replace(new RegExp(stopword, 'gi'), '');
                });
                return str;
            },
            getWords: function () {
                keywordDensity.words = keywordDensity.clearSigns(keywordDensity.clearStopWords(keywordDensity.clearUrls(keywordDensity.clearEntities(keywordDensity.clearHtml(keywordDensity.clearHebrewPoints($("#kw-textarea").val())))))).toLowerCase().replace(/\s+/, ' ')
                    //.split(/[^a-z0-9\u0400-\u052F\u05D0-\u05EA{L}]+/);
                    .split(" ");
                return keywordDensity.words;
            },
            calcTotalWords: function () {
                keywordDensity.tkw = keywordDensity.getWords().length;
                return keywordDensity.tkw;
            },
            calculateDensity: function () {
                var prevWords, wordCount, buckets;

                keywordDensity.calcTotalWords();

                /* count appearances of each word and 2-3 word phrases */
                wordCount = {};
                prevWords = [];
                $.each(keywordDensity.words, function (index, word) {
                    word = $.trim(word);
                    if ($.inArray(word, keywordDensity.stopwords) > -1) {
                        return true; // continue; // don't count stop words
                    }
                    // set a counter for each word in the wordCount hash
                    if (word.length > 2) {
                        if (!wordCount[word]) wordCount[word] = 0;
                        wordCount[word]++;
                    }
                    // add the word to prevWords array
                    prevWords.push(word);
                    var phrase = $.trim(prevWords.join(" "));
                    if (!wordCount[phrase]) wordCount[phrase] = 0;
                    wordCount[phrase]++;
                    if (prevWords.length > 2) prevWords.shift();
                    // another time for the 2 word length phrase
                    phrase = $.trim(prevWords.join(" "));
                    if (!wordCount[phrase]) wordCount[phrase] = 0;
                    wordCount[phrase]++;
                });

                /* drop words into buckets */
                buckets = [];
                for (var key in wordCount) {
                    var value = wordCount[key];
                    var wordlen = key.split(" ").length;
                    if (!buckets[wordlen]) buckets[wordlen] = {};
                    buckets[wordlen][key] = value;
                }
                return buckets;
            },
            addDensity: function (listId, phrase, count) {
                // factor = nb words
                var factor = phrase.split(' ').length;
                var added = false;
                var dens = (factor * count / keywordDensity.tkw) * 100;
                dens = "" + dens;
                dens = dens.substring(0, dens.indexOf('.') + 2);

                var listItems = $(listId + " li");
                listItems.each(function (idx, li) {
                    var cnt = $(li).find('span.kw-count').first().text();
                    if (parseInt(cnt) < count) {
                        $(li).before('<li>x<span class="kw-count">' + count + '</span> - <span class="kw-dens">' + dens + '</span>% - <span class="kw-phrase">' + phrase + '</span></li>');
                        added = true;
                        return false;
                    }
                });

                /*
                 list.children().each(function (idx, opt) {
                 if (parseInt(opt.text.split(' ')[0]) < count) {
                 $(opt).before($("<option>" + count + " - " + dens + "% - " + phrase + "</option>"));
                 added = true;
                 return false;
                 }
                 });
                 */
                if (!added) {
                    //list.append($("<option>" + count + " - " + dens + "% - " + phrase + "</option>"));
                    // Create the inner div before appending to the body
                    $(listId).append('<li>x<span class="kw-count">' + count + '</span> - <span class="kw-dens">' + dens + '</span>% - <span class="kw-phrase">' + phrase + '</span></li>');
                }
            },
            displayWordCount: function () {
                //var wordcounter = $("#wordcounter");
                //wordcounter.html("Word count: " + (tkw - 1));
            },
            phraseWords: [],
            submitKeyPhraseForm: function (e) {
                e.preventDefault();
                var kwInputs = $("#keywordphrase1");

                var keyword = keywordDensity.getKeywords(kwInputs);
                keywordDensity.getPhrasewords(kwInputs);
                // keywords lenght is needed for output
                var keywordsLenght = keyword.length;
                var text = keywordDensity.getWordsPass($("#kw-textarea").val(), keyword);
                var keywordResult = keywordDensity.getKeywordResult(keyword, text);

                console.log('keywordResult : ' + keywordResult);


                var resultArray = keywordDensity.getTop5(text);
                resultArray = keywordDensity.mergeArrays(resultArray, keywordResult);
                resultArray.sort(keywordDensity.sortArray);
                var wordCount = text.length - 1;
                // Clean up the output list
                keywordDensity.cleanList();
                if (wordCount != 0) {
                    keywordDensity.genOutput(wordCount, resultArray, keywordsLenght);
                }
                //console.log(new Date().getTime()-start);
                return false;
            },
            getKeywords: function ($inputs) {
                var ret = [];
                // store all the keywordphrase values in an array
                for (var i = 0; i < $inputs.length; i++) {
                    if ($inputs[i].value.replace(/\s+/, "").length != 0) {
                        ret[ret.length] = $inputs[i].value.toLowerCase();
                    }
                }
                return ret;
            },
            getKeywordResult: function (keyword, text) {
                var res = [];
                var txt = text.join(" ");
                for (var i = 0; i < keyword.length; i++) {
                    var r = new RegExp('\\b' + (keyword[i]
                        // escape special characters in keyword
                        .replace(/([{}()[\]\\.?*+^$|=!:~-])/g, "\\$1")
                        //replace single or multiple white space with a single space
                        .replace(/\s+/igm, " ")) + '\\b', "igm");
                    var counter = 0;
                    while ((r.exec(txt)) != null) {
                        counter++;
                    }
                    counter = keyword[i].replace(/^\s+|\s+$/igm, "")
                        .replace(/\s+/igm, " ").split(" ").length * counter;
                    res[res.length] = [keyword[i], counter, "keyword"];
                }
                return res;
            },
            //stopwords is an array holding words like no yes, it, he, she, a, an ...
            //the should not score as a single word
            removeScore: function (arr) {
                // both arrays are sorted alphabetically
                // so when the current word is bigger than the
                // current stop word, continue at next word at current
                // stop word that's why j is declared here
                var oldj = 0;
                // for each element in the word array that already has
                // score applied to it
                for (var i = 0; i < arr.length; i++) {
                    // for each element in the stopwords array
                    if (keywordDensity.stopwords[j] > arr[i][0]) {
                        continue;
                    }
                    for (var j = oldj; j < keywordDensity.stopwords.length; j++) {
                        if (keywordDensity.stopwords[j] == arr[i][0]) {
                            // current element is the, he, she, a, that...
                            // remove score
                            arr[i][1] = 0;
                            oldj = j;
                            break;
                        }
                    }
                    if (j == keywordDensity.stopwords.length) {
                        // current word is not a stopword, set j to old value
                        j = oldj;
                    }
                }
                return arr;
            },
            getTop5: function (arrWords) {
                // sort the words
                arrWords = arrWords.sort();
                // add an empty element at the end
                // this because current word is checked to be the same
                // as the next one. If the last 2 or more are the same the last
                // words aren't scored
                arrWords[arrWords.length] = "";
                var ret = [];
                // current word will be matched with next item
                var currentWord = arrWords[0];
                var i = 0;
                while (i < arrWords.length) {
                    var wordCounter = 0;
                    // keep going untill current word is not the same as
                    // the next item
                    while (i < arrWords.length && currentWord == arrWords[i]) {
                        // next item is same as current, this allways happens once
                        // since current word needs to be scored even if there is only one
                        wordCounter++;
                        i++;
                    }
                    // arrWords at i is not same as current word
                    // store current word with score in return array
                    ret[ret.length] = [currentWord, wordCounter];
                    // store current word as arrWords at i
                    currentWord = arrWords[i];
                }
                // remove last element added for the while loops to work
                ret = ret.slice(0, ret.length - 1);
                return keywordDensity.removeScore(ret);
            },
            getWordsPass: function (txt, phrases) {
                var val = txt.replace(/(^[^A-Za-z0-9]+)|([^A-Za-z0-9]+$)/ig, "")
                    .replace(/(\d)[,\.](\d)/igm, "$1$2") // replace , with nothing in numbers like 12,000 so 000 doesn't score high
                    .replace(/[^A-Za-z0-9-]+/igm, " ")
                    .replace(/\s+/gmi, " ") + ' ';
                val = val.toLowerCase();
                // replace phrases with a marker
                for (var i = 0; i < phrases.length; i++) {
                    var r = new RegExp('\\b' + (phrases[i]
                        // escape special characters in keyword
                        .replace(/([{}()[\]\\.?*+^$|=!:~-])/g, "\\$1")
                        //replace single or multiple white space with a single space
                        .replace(/\s+/igm, " ")) + '\\b', "igm");
                    val = val.replace(r, "**********" + i + "**********");
                }
                // replace stopwords with nothing
                // using regexp for every stopword is too slow.
                var tmp = val.split(" ");
                for (var i = 0; i < tmp.length; i++) {
                    for (var j = 0; j < keywordDensity.stopwords.length; j++) {
                        if (keywordDensity.stopwords[j] == tmp[i]) {
                            tmp[i] = "";
                        }
                    }
                }
                val = tmp.join(" ");
                //	for(var i=0;i<stopwords.length;i++){
                //		r = new RegExp('\\b'+(stopwords[i]
                //			// escape special characters in keyword
                //			.replace(/([{}()[\]\\.?*+^$|=!:~-])/g, "\\$1"))+'\\b',"igm");
                //		val=val.replace(r,"");
                //	}
                // replace marker with phrases
                for (var i = 0; i < phrases.length; i++) {
                    var r = new RegExp("\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*" + i + "\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*", "igm");
                    val = val.replace(r, phrases[i]);
                }
                // remove any preceding, trailing and double spaces
                val = val.replace(/^\s+|\s+$/igm, "").replace(/\s+/igm, " ") + " ";
                var ret = val.split(" ");
                if (val == ' ') {
                    ret = [];
                }
                return ret.slice(0, ret.length - 1);
            },
            cleanList: function () {
                var li = $("#response li");
                for (var i = 0; i < li.length; i++) {
                    li.get(i).innerHTML = "";
                }

            },

            sortArray: function (a, b) {
                if (a[1] < b[1]) {
                    return 1;
                } else if (b[1] < a[1]) {
                    return -1;
                }
                return 0;
            },
            mergeArrays: function (words, keyWords) {
                for (var i = 0; i < keyWords.length; i++) {
                    var foundMatch = false;
                    for (var j = 0; j < words.length; j++) {
                        if (words[j][0] == keyWords[i][0]) {
                            words[j] = keyWords[i];
                            foundMatch = true;
                            break;
                        }
                    }
                    if (!foundMatch) {
                        words[words.length] = keyWords[i];
                    }
                }
                return words;
            },
            genOutput: function (wordCount, arrResult, keywordsLenght) {
                $("#response h3").show();
                var arrout = [];
                var keyDone = 0;
                arrout[arrout.length] = "<table id=top5keywords><tbody>";
                for (var i = 0; i < arrResult.length; i++) {
                    var percent = new String(Math.ceil((arrResult[i][1] / wordCount) * 1000) / 10);
                    if (percent.indexOf(".") == -1) {
                        percent = percent + ".0";
                    }
                    percent = percent.split(".");
                    if (arrResult[i].length == 3) {
                        //this is one of the keywords
                        arrout[arrout.length] = "<tr><td></td><td>";
                        arrout[arrout.length] = new String(i + 1);
                        arrout[arrout.length] = ".</td><td><span class=\"black\"><b>";
                        arrout[arrout.length] = arrResult[i][0];
                        arrout[arrout.length] = "</b></span>:</td><td class=\"black\">";
                        arrout[arrout.length] = percent[0];
                        arrout[arrout.length] = ".";
                        arrout[arrout.length] = percent[1];
                        arrout[arrout.length] = "%</td><td> (";
                        arrout[arrout.length] = arrResult[i][1];
                        arrout[arrout.length] = ")</td></tr>";
                        keyDone++;
                        if (keyDone == keywordsLenght && i >= 5) {
                            break;
                        }
                        continue;
                    }
                    if (i < 5 && arrResult[i][1] != 0) {
                        arrout[arrout.length] = "<tr><td></td><td>";
                        arrout[arrout.length] = new String(i + 1);
                        arrout[arrout.length] = ".</td><td><span class=\"black\">";
                        arrout[arrout.length] = arrResult[i][0];
                        arrout[arrout.length] = "</span>:</td><td class=\"black\">";
                        arrout[arrout.length] = percent[0];
                        arrout[arrout.length] = ".";
                        arrout[arrout.length] = percent[1];
                        arrout[arrout.length] = "%</td><td> (";
                        arrout[arrout.length] = arrResult[i][1];
                        arrout[arrout.length] = ")</td></tr>";
                    }
                }
                arrout[arrout.length] = "</tbody></table>";
                $("#response div").html(arrout.join(""));
            },
            getPhrasewords: function (inputs) {
                keywordDensity.phraseWords = [];
                for (var i = 0; i < inputs.length; i++) {
                    var val = inputs.get(i).value.replace(/\s+/igm, " ").replace(/^\s+|\s+$/igm, "");
                    if (val != "") {
                        keywordDensity.phraseWords[keywordDensity.phraseWords.length] = val;
                    }
                }
            },
            resetDensityUi: function () {
                clearTimeout(keywordDensity.delay);
                $('#kw-textarea').val('');
                var singleword = "#kw-density-results-one";
                var doubleword = "#kw-density-results-two";
                var tripleword = "#kw-density-results-three";

                $(singleword).empty();
                $(doubleword).empty();
                $(tripleword).empty();
            },
            displayDensity: function () {
                clearTimeout(keywordDensity.delay);

                var singleword = "#kw-density-results-one";
                var doubleword = "#kw-density-results-two";
                var tripleword = "#kw-density-results-three";

                var densities = keywordDensity.calculateDensity();

                $(singleword).empty();
                $(doubleword).empty();
                $(tripleword).empty();

                for (var word in densities[1]) {
                    keywordDensity.addDensity(singleword, word, densities[1][word]);
                }
                for (var phrase2 in densities[2]) {
                    keywordDensity.addDensity(doubleword, phrase2, densities[2][phrase2]);
                }
                for (var phrase3 in densities[3]) {
                    keywordDensity.addDensity(tripleword, phrase3, densities[3][phrase3]);
                }

                keywordDensity.displayWordCount();
            }
        };

        var googleTrends = {
            init: function () {
                // tagedit on google trends search
                var localJSON = [
                    { "id": "0", "label": "|", "value": "|" }
                ];

                $("#google_trends_keywords_input").tagedit({
                    autocompleteOptions: {
                        source: localJSON,
                        deleteEmptyItems: true
                    }
                });
            },
            resetGoogleTrendsForm: function (e) {
                e.preventDefault();
                $("div#google-trends-result").empty();
                // remove all ul li inside form#google-trends-form having class 'tagedit-listelement-old'
                var listItemsToRemove = $("form#google-trends-form ul.tagedit-list  li.tagedit-listelement-old");
                listItemsToRemove.each(function (idx, li) {
                    //$(li).find('span.proxy-data-host').first().text();
                    $(li).remove();
                });

            }, submitGoogleTrendsForm: function (e) {
                e.preventDefault();
                var $form = $("#google-trends-form");
                var data = $form.serialize();
                //console.log("form data = " + data);
                if (data) {
                    var tmp1 = data.split("&");
                    var tags = [];
                    for (var k = 0; k < tmp1.length; k++) {
                        if (tmp1[k] && tmp1[k] !== "" && tmp1[k] !== " ") {
                            tags.push(tmp1[k].split("=")[1]);
                            //console.log("tag = " + tags[k]);
                        }
                    }
                    var urlTrends = 'https://trends.google.com/trends/explore?date=all&q=' + tags.join() + '&hl=en-US';
                    window.open(urlTrends, 'windowOpenTab', 'toolbar=0,menubar=0,scrollbars=1,location=no,resizable=1,width=850,height=600,left=0, top=0');
                    //window.open(urlTrends, '_blank');

                    /*
                     var resHtml = "<div><a class='btn btn-lg btn-info' href='https://trends.google.com/trends/explore?date=all&q=" + tags.join() + "&hl=en-US' target='_blank'>Open in Google Trends</a></div>";

                     var urlCompare = "http://www.google.com/trends/fetchComponent?hl=en-US&q=" + tags.join() + "&cmpt=q&content=1&cid=TIMESERIES_GRAPH_0&export=5&w=640&h=330";
                     var htmlCompare = "<iframe scrolling='no' style='border:none;' width='640' height='295' src='" + urlCompare + "'></iframe>";

                     resHtml += htmlCompare;

                     for (var p = 0; p < tags.length; p++) {
                     var urlCountry = "http://www.google.com/trends/fetchComponent?hl=en-US&q=" + tags[p] + "&cmpt=q&content=1&cid=GEO_MAP_0_0&export=5&w=640&h=330";
                     var htmlCountry = "<iframe scrolling='no' style='border:none;background: #F9F9F9;' width='640' height='395' src='" + urlCountry + "'></iframe>";
                     resHtml += htmlCountry;
                     }

                     $("div#google-trends-result").empty().html(resHtml);
                     */
                } else {
                    notify("Please add some keywords before submit!", "error", 5000);
                }
            }
        };

        KeywordToolsApi.initUi = function () {
            getInstance();
            ui.init();
        };
        KeywordToolsApi.resetUnicodeDetector = function () {
            unicodeDetector.resetUnicodeDetector();
        };
        KeywordToolsApi.resetGoogleTrendsForm = function (e) {
            googleTrends.resetGoogleTrendsForm(e);
        };
        KeywordToolsApi.submitGoogleTrendsForm = function (e) {
            googleTrends.submitGoogleTrendsForm(e);
        };
        KeywordToolsApi.resetDensityUi = function () {
            keywordDensity.resetDensityUi();
        };
        KeywordToolsApi.displayDensity = function () {
            keywordDensity.displayDensity();
        };
        return KeywordToolsApi;
    };

    exports.MKeywordTools = MKeywordTools;
    window.KeywordToolsModule = new MKeywordTools;
}(this, jQuery);

$(function () {
    KeywordToolsModule.initUi();
});