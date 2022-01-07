let descriptionElems = document.getElementsByClassName("fact_description");
for (let elem of descriptionElems) {
    let outputStringArray = [];
    let startIndex = 0;
    let textContent = elem.textContent;
    let matches = [...textContent.matchAll(/\[\d+\]/g)];
    if (matches.length > 0) {
        matches.forEach(match => {
            let matchedString = match[0];
            let matchedStringLength = matchedString.length;
            let matchedStringNum = matchedString.slice(1, -1);
            let matchedIndex = match.index;
            let goodString = textContent.slice(startIndex, matchedIndex);
            let goodStringLength = goodString.length;
            startIndex += goodStringLength + matchedStringLength;
            outputStringArray.push(goodString);
            outputStringArray.push("<sup>", matchedStringNum, "</sup>");
        })
        elem.innerHTML = outputStringArray.join("");
    }
}