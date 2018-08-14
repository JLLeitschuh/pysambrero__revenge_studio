importScripts('../../public/plugins/pako/pako.min.js');

var chunk_size = 170000;


self.onmessage = function (msg) {
    var dataWorker = msg.data;
    switch (dataWorker.cmd) {
        case 'sync_file':
            //console.log('worker is executing sync_file');
            var content, is_compressed, is_chunked;
            content = btoa(pako.gzip(dataWorker.content.split("\n").join("%%br%%"), { to: 'string' }));
            is_compressed = true;
            is_chunked = false;
            var orig_length = dataWorker.content.length;
            var compressed_length = content.length;
            console.log("original["+orig_length+"], compressed [" + compressed_length+ "} ==> compression rate = " + Math.ceil((compressed_length*100)/orig_length) + "%");

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
                    chunk['total'] = nbChunks; // total chunks for this file
                    chunks.push(chunk);
                }
                is_chunked = true;
                content = chunks;
                chunks = null;
            }
            self.postMessage({content: content, is_compressed: is_compressed, is_chunked: is_chunked});
            content = null;
            break;
        case 'stop':
            self.close(); // Terminates the worker.
            break;
        default:
            self.postMessage('Unknown command: ' + dataWorker.msg);
    }
};

// Generate a UUID
function generateUUID() {
    var d = new Date().getTime();
    return  'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}