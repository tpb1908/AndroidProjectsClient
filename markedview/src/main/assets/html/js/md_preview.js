function touchStart(event) {
    TouchIntercept.beginTouchIntercept();
}

function touchEnd(event) {
    TouchIntercept.endTouchIntercept();
}

function preview(md_text, codeScrollDisable) {
    if(md_text == "") {
        return false;
    }
    document.getElementById("preview").innerHTML = md_text.replace(/\\n/g, "\n")
    var codes = document.getElementsByClassName('code');
    for(var i = 0; i < codes.length; i++) {
        codes[i].style.display = 'block';
        codes[i].style.wordWrap = 'normal';
        codes[i].style.overflowX = 'scroll';
        if(!codes[i].innerHTML.includes("license")) {
            hljs.highlightBlock(codes[i]);
        }
                codes[i].addEventListener("touchstart", touchStart, false);
                codes[i].addEventListener("touchend", touchEnd, false)
    }

    var pres = document.getElementsByTagName('pre');
    for(var i = 0; i < pres.length; i++) {
        pres[i].addEventListener("touchstart", touchStart, false);
        pres[i].addEventListener("touchend", touchEnd, false)
    }
    var tables = document.getElementsByTagName('table');
    for(var i = 0; i < tables.length; i++) {
        tables[i].addEventListener("touchstart", touchStart, false);
        tables[i].addEventListener("touchend", touchEnd, false)
    }

}