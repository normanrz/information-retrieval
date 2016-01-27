{ function flatten(node) {
    if (node.type == 'and' || node.type == 'or') {
    var values = [];
      node.values.forEach(function (value) {
        if (value.type == node.type) {
          values.push.apply(values, value.values);
        } else {
          values.push(value);
        }
      });
      node.values = values;
      return node;
    } else {
      return node;
    }
} }


Query
  = _ query:Subquery not:( _ NotToken )* prf:(_ Prf)? _
  { return { 
      query: query, 
      not: not.map(function (t) { return t[1] }), 
      prf: prf ? prf[1] : 0
    }; } 

Subquery
  = boolean:BooleanOr
  { return boolean; }

Keyword = "AND" / "OR" / "NOT"

Word
  = !Keyword [a-zA-Z0-9]+ { return text(); }

Number
  = [0-9]+ { return parseInt(text()); }

Token
  = word:Word asterisk:("*")?
  { return { type: 'token', value: word, isPrefix: !!asterisk }; }


LinkTo
  = "linkTo:" number:Number
  { return {
      type: 'link',
      value: parseInt(number)
    }; }

Phrase
  = "\"" head:Token tail:( __ Token )* "\"" 
  { return { 
      type: 'phrase', 
      values: [head].concat(tail.map(function (t) { return t[1]; })) 
    }; }

TokenOrPhrase
  = LinkTo / Token / Phrase
  
NotToken
  = "NOT" _ value:TokenOrPhrase
  { return value }
  
Prf
  = "#" number:Number 
  { return number; }

BooleanAnd
  = left:Primary __ "AND" __ right:BooleanAnd
  { return flatten({ type: 'and', values: [left, right] }); }
  / Primary

BooleanOr
  = (left:BooleanAnd __ ("OR" __)? right:BooleanOr)
  { return flatten({ type: 'or', values: [left, right] }); }
  / BooleanAnd

Primary
  = value:TokenOrPhrase
  / "(" _ value:BooleanOr _ ")"
  { return value; }

_ "optionalWhitespace"
  = [ \t\n\r]*

__ "whitespace"
  = [ \t\n\r]+