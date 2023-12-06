import React from 'react'

const Literate = ({ children }) => {
  // see https://stackoverflow.com/a/59431317
  const StyledChildren = () =>
    React.Children.map(children, child => {
        const newClassName =
          child.props.className === undefined ?
            "literate-coding" : `${child.props.className} ${"literate-coding"}`;
        return React.cloneElement(child, {
          className: newClassName
        })
      }
    );

  return <StyledChildren />;
}

export default Literate
