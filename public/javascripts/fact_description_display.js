let descriptionElems = document.getElementsByClassName("fact_description");
for (let elem of descriptionElems) {
    let outputStringArray = [];
    let startIndex = 0;
    let textContent = elem.textContent;
    let matches = [...textContent.matchAll(/\[\d+\]/g)];
    let matchNum = 0;
    if (matches.length > 0) {
        matches.forEach(match => {
            matchNum += 1;
            let matchedString = match[0];
            let matchedStringLength = matchedString.length;
            let matchedStringNum = matchedString.slice(1, -1);
            let matchedIndex = match.index;
            let goodString = textContent.slice(startIndex, matchedIndex);
            let goodStringLength = goodString.length;
            startIndex += goodStringLength + matchedStringLength;
            outputStringArray.push(goodString);
            outputStringArray.push("<sup>", matchedStringNum, "</sup>");
            if (matchNum === matches.length) {
                outputStringArray.push(textContent.slice(startIndex));
            }
        })
        elem.innerHTML = outputStringArray.join("");
    }
}