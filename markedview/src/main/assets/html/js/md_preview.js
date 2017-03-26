$(function() {

    preview = function setMarkdown(md_text, codeScrollDisable) {
        if(md_text == ""){
          return false;
        }

        $('#preview').html(md_text.replace(/\\n/g, "\n"));

        $('pre code').each(function(i, block) {
            if(!block.innerHTML.includes("license")) {
                hljs.highlightBlock(block);
            }
        });

    };
});